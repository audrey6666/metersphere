<template>
  <el-popover placement="bottom-end" width="392" height="394" trigger="click">
    <div class="version-history-wrap">
      <div class="label-row">
        <div class="label">{{ $t("project.version.name") }}</div>
      </div>
      <div class="history-container">
        <div class="item-row" v-for="item in versionOptions" :key="item.id">
          <div class="left-detail-row">
            <div class="version-info-row">
              <div
                :class="
                  item.id == dataLatestId
                    ? ['version-label', 'active']
                    : ['version-label']
                "
              >
                {{ item.name }}
              </div>
              <div class="version-lasted" v-if="item.id == dataLatestId">
                {{ $t("api_test.api_import.latest_version") }}
              </div>
            </div>
            <div class="version-detail">
              <div class="creator">{{ item.createUserName }} 创建</div>
            </div>
          </div>
          <div
            class="right-opt-row"
            v-if="item.isCheckout || item.status !== 'open'"
          >
            <div
              class="updated opt-row"
              @click="setLatest(item)"
              v-if="
                hasLatest &&
                item.isCheckout &&
                !(isRead || item.id === dataLatestId)
              "
            >
              {{ $t("置新") }}
            </div>
            <div
              class="checkout opt-row"
              @click="checkout(item && !isCurrent)"
              v-if="item.isCheckout"
            >
              {{ $t("project.version.checkout") }}
            </div>
            <div
              class="create opt-row"
              v-if="!item.isCheckout && item.status === 'open' && !isRead"
              @click="create(item)"
            >
              {{ $t("commons.create") }}
            </div>
            <div
              class="delete opt-row"
              @click="del(item)"
              v-if="item.isCheckout && !(item.isCurrent || isRead)"
            >
              {{ $t("commons.delete") }}
            </div>
            <div
              @click="compare(item)"
              v-if="item.isCheckout && !item.isCurrent"
            >
              {{ $t("project.version.compare") }}
            </div>
          </div>
        </div>
      </div>
      <div class="compare-row" v-if="false">
        <div class="icon">
          <img src="/assets/figma/icon_contrast_outlined.svf" alt="" />
        </div>
        <div class="label">{{ $t("版本对比") }}</div>
      </div>
    </div>

    <!-- origin -->
    <el-table
      :data="versionOptions"
      v-loading="loading"
      height="200px"
      v-if="false"
    >
      <el-table-column
        prop="name"
        :label="$t('project.version.name')"
        show-overflow-tooltip
      >
        <template v-slot:default="scope">
          <font-awesome-icon
            v-if="scope.row.isCurrent"
            class="icon global focusing"
            :icon="['fas', 'tag']"
          />
          {{ scope.row.name }}
          <el-tag
            v-if="scope.row.id === dataLatestId"
            size="mini"
            type="primary"
          >
            {{ $t("api_test.api_import.latest_version") }}&nbsp;
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column
        prop="status"
        column-key="status"
        min-width="65"
        :label="$t('commons.status')"
      >
        <template v-slot:default="{ row }">
          <el-tag size="mini" type="primary" v-if="row.status === 'open'">
            {{ $t("project.version.version_open") }}
          </el-tag>
          <el-tag
            size="mini"
            type="info"
            effect="plain"
            v-else-if="row.status === 'closed'"
          >
            {{ $t("project.version.version_closed") }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createUser" :label="$t('operating_log.user')">
        <template v-slot:default="scope">
          <span>{{
            userData[scope.row.createUser]
              ? userData[scope.row.createUser]
              : scope.row.createUser
          }}</span>
        </template>
      </el-table-column>
      <el-table-column :label="$t('commons.operating')" min-width="70px">
        <template v-slot:default="scope">
          <el-link
            @click="compare(scope.row)"
            v-if="scope.row.isCheckout"
            :disabled="scope.row.isCurrent"
          >
            {{ $t("project.version.compare") }}&nbsp;
          </el-link>

          <el-link
            @click="checkout(scope.row)"
            v-if="scope.row.isCheckout"
            :disabled="scope.row.isCurrent"
          >
            {{ $t("project.version.checkout") }}&nbsp;
          </el-link>

          <el-link
            v-if="!scope.row.isCheckout && scope.row.status === 'open'"
            @click="create(scope.row)"
            :disabled="isRead"
          >
            {{ $t("commons.create") }}&nbsp;
          </el-link>

          <el-popover
            placement="bottom"
            width="100"
            trigger="hover"
            v-if="scope.row.isCheckout || scope.row.status !== 'open'"
          >
            <div style="text-align: left">
              <el-link
                @click="setLatest(scope.row)"
                v-if="hasLatest && scope.row.isCheckout"
                :disabled="isRead || scope.row.id === dataLatestId"
              >
                {{ $t("project.version.set_new") }}&nbsp;
              </el-link>
              <br />
              <el-link
                @click="del(scope.row)"
                v-if="scope.row.isCheckout"
                :disabled="scope.row.isCurrent || isRead"
              >
                {{ $t("commons.delete") }}&nbsp;
              </el-link>
            </div>
            <span slot="reference">...</span>
          </el-popover>
        </template>
      </el-table-column>
    </el-table>
    <span slot="reference">
      <slot
        name="versionLabel"
        v-if="versionEnable && currentVersion.id"
      ></slot>
    </span>
  </el-popover>
</template>

<script>
import { getCurrentProjectID } from "metersphere-frontend/src/utils/token";
import { hasLicense } from "metersphere-frontend/src/utils/permission";
import {
  getDefaultVersion,
  getProjectMembers,
  getProjectVersions,
  isProjectVersionEnable,
} from "metersphere-frontend/src/api/version";

export default {
  name: "CaseVersionHistory",
  props: {
    versionData: Array,
    currentId: String,
    testUsers: Array,
    useExternalUsers: Boolean,
    isTestCaseVersion: {
      type: Boolean,
      default: false,
    },
    isRead: {
      type: Boolean,
      default: false,
    },
    currentProjectId: {
      type: String,
      default() {
        return getCurrentProjectID();
      },
    },
    hasLatest: {
      type: Boolean,
      default: false,
    },
  },
  data() {
    return {
      loading: false,
      versionEnable: false,
      versionOptions: [],
      userData: {},
      currentVersion: {},
      dataLatestId: "",
    };
  },
  methods: {
    getVersionOptionList(callback) {
      getProjectVersions(this.currentProjectId).then((response) => {
        this.versionOptions = response.data.filter((v) => v.status === "open");
        if (callback) {
          callback(this.versionOptions);
        }
      });
    },
    updateUserDataByExternal() {
      if (this.testUsers && this.testUsers.length > 0) {
        this.testUsers.forEach((u) => {
          this.userData[u.id] = u.name;
        });
      }
    },
    getUserOptions() {
      if (this.useExternalUsers) {
        this.updateUserDataByExternal();
      } else {
        getProjectMembers().then((response) => {
          this.userOptions = response.data;
          this.userOptions.forEach((u) => {
            this.userData[u.id] = u.name;
          });
        });
      }
    },
    compare(row) {
      this.$emit("compare", row);
    },
    checkout(row) {
      this.loading = true;
      this.$emit("checkout", row);
    },
    create(row) {
      this.loading = true;
      this.$emit("create", row);
    },
    del(row) {
      this.loading = true;
      this.$emit("del", row);
    },
    setLatest(row) {
      this.loading = true;
      this.$emit("setLatest", row);
    },
    handleVersionOptions() {
      let versionData = this.versionData;
      if (versionData.length === 0) {
        this.currentVersion =
          this.versionOptions.filter(
            (v) => v.status === "open" && v.latest
          )[0] || {};
        this.loading = false;
        return;
      }
      let latestData = versionData.filter((v) => v.latest === true);
      if (latestData) {
        this.dataLatestId = latestData[0].versionId;
      }
      this.versionOptions.forEach((version) => {
        let vs = versionData.filter((v) => v.versionId === version.id);
        version.isCheckout = vs.length > 0; // 已存在可以切换，不存在则创建
        if (version.isCheckout) {
          version.createUser =
            vs[0].createUser || vs[0].userId || vs[0].creator;
        } else {
          version.createUser = null;
        }
        let lastItem = versionData.filter((v) => v.id === this.currentId)[0];
        if (lastItem) {
          version.isCurrent = lastItem.versionId === version.id;
          if (version.isCurrent) {
            this.currentVersion = version;
          }
        }
      });
      this.loading = false;
    },
  },
  watch: {
    versionData() {
      if (!hasLicense()) {
        return;
      }
      isProjectVersionEnable(this.currentProjectId).then((response) => {
        this.versionEnable = response.data;
      });
      this.getUserOptions();
      this.getVersionOptionList(this.handleVersionOptions);
    },
    testUsers() {
      this.updateUserDataByExternal();
    },
  },
};
</script>

<style scoped lang="scss">
@import "@/business/style/index.scss";
:deep(.el-popover) {
  width: 392px !important;
}
.version-history-wrap {
  width: 392px;
  height: 271px;
  .label-row {
    height: 32px;
    line-height: 32px;
    margin-top: 8px;
    .label {
      font-weight: 500;
      color: #1f2329;
      width: 100px;
      height: 22px;
      line-height: 22px;
      margin-left: 11px;
    }
  }

  .history-container {
    height: 182px;
    overflow: scroll;
    .item-row:hover {
      background: rgba(31, 35, 41, 0.1);
    }
    .item-row {
      cursor: pointer;
      margin-top: 8px;
      height: 50px;
      display: flex;
      padding: 0 11px;
      justify-content: space-between;
      .left-detail-row {
        display: flex;
        flex-direction: column;
        justify-content: center;
        .version-info-row {
          display: flex;
          .version-label {
            height: 22px;
            font-weight: 500;
            font-size: 14px;
            line-height: 22px;
            color: #1f2329;
          }

          .version-lasted {
            height: 20px;
            margin-left: 4px;
            background: rgba(120, 56, 135, 0.2);
            border-radius: 2px;
            padding: 1px 6px;
            color: #783887;
            text-align: center;
            line-height: 20px;
          }
        }

        .version-detail {
          .creator {
            height: 20px;
            font-size: 12px;
            line-height: 20px;
            color: #8f959e;
          }
        }
      }

      .right-opt-row {
        display: flex;
        justify-content: flex-end;
        align-items: center;
        .opt-row:not(:first-child) {
          margin-left: 16px;
        }
        .opt-row {
          height: 22px;
          line-height: 22px;
          font-size: 14px;
          text-align: center;
          color: #646a73;
          cursor: pointer;
        }
        .opt-row:hover {
          background: rgba(120, 56, 135, 0.1);
          border-radius: 4px;
          cursor: pointer;
        }
        .updated {
        }

        .create {
        }

        .delete {
        }
      }
    }
  }
  .active {
    color: #783887;
  }
  .compare-row {
    display: flex;
    height: 32px;
    line-height: 32px;
    margin-bottom: 8px;
    margin-top: 3px;
    border-top: 1px solid rgba(31, 35, 41, 0.15);
    align-items: center;
    .icon {
      margin-left: 11.67px;
      margin-right: 4.6px;
      width: 14.67px;
      height: 13.33px;
      img {
        width: 100%;
        height: 100%;
      }
    }

    .label {
      font-size: 14px;
      color: #646a73;
    }
  }
}
</style>
