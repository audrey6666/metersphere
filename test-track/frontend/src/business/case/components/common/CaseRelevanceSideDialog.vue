<template>
  <div>
    <el-drawer
      :close-on-click-modal="false"
      :visible.sync="visible"
      :size="width"
      @close="close"
      destroy-on-close
      :full-screen="isFullScreen"
      ref="relevanceDialog"
      custom-class="file-drawer"
      append-to-body
    >
      <template slot="title">
        <div style="color: #1f2329; font-size: 16px; font-weight: 500">
          {{ dialogTitle }}
        </div>
      </template>
      <!-- todo -->
      <template slot="headerBtn" v-if="$slots.headerBtn">
        <div>
          <slot name="headerBtn"></slot>
        </div>
      </template>
      <!-- <template slot="title" slot-scope="{ title }" v-if="!$slots.headerBtn">
        <ms-dialog-header
          :title="title"
          :enable-cancel="false"
          @confirm="save"
          btn-size="mini"
          @fullScreen="isFullScreen = !isFullScreen"
        >
          <template #other>
            <table-select-count-bar
              :count="selectCounts"
              style="float: left; margin: 5px"
            />

            <div v-if="flag" style="margin: 5px; float: left">
              <el-checkbox v-model="checked" class="el-checkbox__label">{{
                $t("test_track.sync_add_api_load")
              }}</el-checkbox>
            </div>
          </template>
        </ms-dialog-header>
      </template> -->

      <div class="body-wrap">
        <div class="aside-wrap">
          <span v-if="isAcrossSpace" class="menu-title">{{
            "[" + $t("project.version.checkout") + $t("commons.space") + "]"
          }}</span>
          <el-select
            v-if="isAcrossSpace"
            filterable
            slot="prepend"
            v-model="workspaceId"
            @change="changeWorkspace"
            class="ms-header-workspace"
            size="small"
          >
            <el-option
              v-for="(item, index) in workspaceList"
              :key="index"
              :label="item.name"
              :value="item.id"
            />
          </el-select>
          <select-menu
            :data="projects"
            v-if="multipleProject"
            width="155px"
            :current-data="currentProject"
            :title="$t('test_track.switch_project')"
            @dataChange="changeProject"
          />
          <slot name="aside"> </slot>
        </div>

        <div class="content-wrap">
          <slot></slot>
        </div>
      </div>
      <div class="footer-wrap">
        <slot name="options">
          <div class="options">
            <div class="options-btn">
              <div class="check-row" v-if="selectCounts > 0">
                <div class="label">已选择 {{ selectCounts }} 条</div>
                <div class="clear" @click="clearSelect">清空</div>
              </div>
              <div class="cancel">
                <el-button size="small" @click="visible = false">{{
                  $t("commons.cancel")
                }}</el-button>
              </div>
              <div class="submit">
                <el-button
                  size="small"
                  v-prevent-re-click
                  :type="selectCounts > 0 ? 'primary' : 'info'"
                  @click="save"
                  @keydown.enter.native.prevent
                >
                  {{ $t("commons.confirm") }}
                </el-button>
              </div>
            </div>
          </div>
        </slot>
      </div>
    </el-drawer>
  </div>
</template>

<script>
import MsDialogHeader from "metersphere-frontend/src/components/MsDialogHeader";
import SelectMenu from "@/business/common/SelectMenu";
import {
  getCurrentProjectID,
  getCurrentUserId,
  getCurrentWorkspaceId,
} from "metersphere-frontend/src/utils/token";
import { getUserWorkspaceList } from "metersphere-frontend/src/api/workspace";
import { getUserProjectList } from "metersphere-frontend/src/api/project";
import TableSelectCountBar from "metersphere-frontend/src/components/table/MsTableSelectCountBar";

export default {
  name: "CaseRelevanceSideDialog",
  components: {
    SelectMenu,
    MsDialogHeader,
    TableSelectCountBar,
  },
  data() {
    return {
      checked: true,
      currentProject: {},
      projectId: "",
      projectName: "",
      projects: [],
      workspaceId: "",
      workspaceList: [],
      currentWorkSpaceId: "",
      selectCounts: null,
      isFullScreen: false,
      visible: false,
    };
  },
  props: {
    planId: {
      type: String,
    },
    dialogTitle: {
      type: String,
      default() {
        return this.$t("test_track.plan_view.relevance_test_case");
      },
    },
    flag: {
      type: Boolean,
    },
    width: {
      type: Number,
      default: 1200,
    },
    isSaving: {
      type: Boolean,
      default() {
        return false;
      },
    },
    isAcrossSpace: {
      type: Boolean,
      default() {
        return false;
      },
    },
    multipleProject: {
      type: Boolean,
      default: true,
    },
  },
  methods: {
    clearSelect() {
      this.$emit("clearSelect");
    },

    refreshNode() {
      this.$emit("refresh");
    },

    save() {
      this.$emit("save", this.checked);
    },

    close() {
      this.visible = false;
    },

    open() {
      this.workspaceId = getCurrentWorkspaceId();
      this.getProject();
      this.selectCounts = null;
      this.visible = true;
    },

    getProject() {
      let realWorkSpaceId = this.isAcrossSpace
        ? this.workspaceId
        : this.currentWorkSpaceId;
      getUserProjectList({
        userId: getCurrentUserId(),
        workspaceId: realWorkSpaceId,
      }).then((res) => {
        let data = res.data;
        if (data && data.length > 0) {
          const index = data.findIndex((d) => d.id === getCurrentProjectID());
          this.projects = data;
          if (index !== -1) {
            this.projectId = data[index].id;
            this.projectName = data[index].name;
            this.changeProject(data[index]);
          } else {
            this.projectId = data[0].id;
            this.projectName = data[0].name;
            this.changeProject(data[0]);
          }
        } else {
          this.$message.warning(
            this.$t("commons.current_workspace") +
              this.$t("commons.not_exist") +
              this.$t("commons.project") +
              "!"
          );
        }
      });
    },
    changeProject(project) {
      if (project) {
        this.currentProject = project;
        this.$emit("setProject", project.id);
        // 获取项目时刷新该项目模块
        this.$emit("refreshNode");
      }
    },
    getWorkSpaceList() {
      getUserWorkspaceList().then((r) => {
        this.workspaceList = r.data;
      });
    },
    changeWorkspace() {
      this.getProject();
    },
  },
  created() {
    this.currentWorkSpaceId = getCurrentWorkspaceId();
    this.workspaceId = this.currentWorkSpaceId;
    if (this.isAcrossSpace) {
      this.getWorkSpaceList();
    }
  },
};
</script>

<style scoped>
.menu-title {
  color: darkgrey;
  margin-left: 10px;
  margin-right: 10px;
}

.ms-header-workspace {
  width: 155px;
  margin-bottom: 10px;
}
</style>
<style scoped lang="scss">
@import "@/business/style/index.scss";
.body-wrap {
  display: flex;
  height: px2rem(763);
  .aside-wrap {
    width: px2rem(268);
    border-right: 1px solid rgba(31, 35, 41, 0.15);
    padding: px2rem(24) px2rem(24) 0 px2rem(24);
  }
  .content-wrap {
    width: px2rem(930);
  }
}
.footer-wrap {
  width: 100%;
  height: px2rem(80);
  background: #ffffff;
  box-shadow: 0px -1px 4px rgba(31, 35, 41, 0.1);
}

.footer-wrap .options {
  height: 80px;
  background: #ffffff;
  box-shadow: 0px -1px 4px rgba(31, 35, 41, 0.1);
  overflow: hidden;
}
.footer-wrap .options-btn {
  display: flex;
  margin-top: 24px;
  height: 32px;
  margin-right: 24px;
  float: right;
}
.footer-wrap .options-btn .submit {
  margin-left: 12px;
}
.footer-wrap .check-row {
  display: flex;
  line-height: 32px;
}
.check-row .label {
  font-family: "PingFang SC";
  font-style: normal;
  font-weight: 400;
  font-size: 14px;
  text-align: center;
  color: #646a73;
}
.check-row .clear {
  font-family: "PingFang SC";
  font-style: normal;
  font-weight: 400;
  font-size: 14px;
  text-align: center;
  color: #783887;
  cursor: pointer;
  margin-left: 16px;
  margin-right: 16px;
}
</style>
