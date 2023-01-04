<template>
  <div class="content-body-wrap">
    <!-- 非创建状态下 展示 -->
    <div class="tab-pane-wrap" v-if="!editable">
      <el-tabs v-model="caseActiveName">
        <el-tab-pane label="用例详情" name="detail">
          <div class="content-conatiner">
            <case-detail-component
              :editable="editable"
              :form="form"
              :richTextDefaultOpen="richTextDefaultOpen"
              :formLabelWidth="formLabelWidth"
            ></case-detail-component>
          </div>
          <div class="comment-common">
            <case-comment-component :case-id="caseId"></case-comment-component>
          </div>
        </el-tab-pane>
        <el-tab-pane label="关联测试用例" name="associateTestCases">
          <div class="content-conatiner">关联测试用例</div>
          <div class="comment-common">
            <case-comment-component :case-id="caseId"></case-comment-component>
          </div>
        </el-tab-pane>
        <!-- 关联缺陷 -->
        <el-tab-pane
          :label="$t('test_track.case.relate_issue')"
          name="associatedDefects"
        >
          <div class="content-conatiner">
            <case-issue-relate
              :plan-id="planId"
              :is-copy="isCopy"
              :read-only="readOnly && !isTestPlan"
              :plan-case-id="planId ? this.form.id : null"
              :case-id="caseId"
              ref="issue"
            ></case-issue-relate>
          </div>
          <div class="comment-common">
            <case-comment-component :case-id="caseId"></case-comment-component>
          </div>
        </el-tab-pane>
        <el-tab-pane label="依赖关系" name="dependencies">
          <div class="content-conatiner">依赖关系</div>
          <div class="comment-common">
            <case-comment-component :case-id="caseId"></case-comment-component>
          </div>
        </el-tab-pane>
        <el-tab-pane label="评论" name="comment">
          <div class="content-conatiner">
            <case-comment-viewer
              @getComments="getComments"
              :comments="comments"
            ></case-comment-viewer>
          </div>
          <div class="comment-common">
            <case-comment-component :case-id="caseId"></case-comment-component>
          </div>
        </el-tab-pane>
        <!-- 变更记录 -->
        <el-tab-pane label="变更记录" name="changeRecord">
          <div class="content-conatiner">
            <case-change-history></case-change-history>
          </div>
          <div class="comment-common">
            <case-comment-component :case-id="caseId"></case-comment-component>
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>
    <case-detail-component
      v-if="editable"
      :editable="editable"
      :form="form"
      :richTextDefaultOpen="richTextDefaultOpen"
      :formLabelWidth="formLabelWidth"
      ref="testCaseBaseInfo"
    ></case-detail-component>
  </div>
</template>

<script>
import BaseEditItemComponent from "../BaseEditItemComponent";
import CaseDetailComponent from "./CaseDetailComponent";
import CaseBaseInfo from "./CaseBaseInfo";
import CaseChangeHistory from "./CaseChangeHistory";
import CaseIssueRelate from "./CaseIssueRelate";
import CaseCommentComponent from "./CaseCommentComponent";
import CaseCommentViewer from "./CaseCommentViewer";
import { getRelationshipCountCase } from "@/api/testCase";
import { testCaseCommentList } from "@/api/test-case-comment";
export default {
  name: "CaseEditInfoComponent",
  components: {
    CaseIssueRelate,
    CaseChangeHistory,
    CaseBaseInfo,
    CaseDetailComponent,
    BaseEditItemComponent,
    CaseCommentComponent,
    CaseCommentViewer,
  },
  props: [
    "richTextDefaultOpen",
    "formLabelWidth",
    "editable",
    // other
    "form",
    "labelWidth",
    "caseId",
    "readOnly",
    "projectId",
    "isTestPlan",
    "planId",
    "versionEnable",
    "isCopy",
    "copyCaseId",
    "type",
    "comments",
    "isClickAttachmentTab",
    "defaultOpen",
  ],
  data() {
    return {
      // since v2.6
      caseActiveName: "detail",

      result: {},
      tabActiveName: "remark",
      fileList: [],
      tableData: [],
      demandOptions: [],
      relationshipCount: 0,
      demandValue: [],
      demandLabel: "",
      //sysList:this.sysList,//一级选择框的数据
      props: {
        multiple: true,
        //lazy: true,
        //lazyLoad:this.lazyLoad
      },
      intervalMap: new Map(),
      cancelFileToken: [],
      uploadFiles: [],
      relateFiles: [],
      unRelateFiles: [],
      dumpFile: {},
    };
  },
  computed: {
    isTesterPermission() {
      return true;
    },
  },
  watch: {
    tabActiveName() {
      if (this.tabActiveName === "demand") {
        this.getDemandOptions();
      } else if (this.tabActiveName === "bug") {
        this.$nextTick(() => {
          this.$refs.issue.getIssues();
        });
      } else if (this.tabActiveName === "relationship") {
        this.$refs.relationship.open();
      } else if (this.tabActiveName === "attachment") {
        this.getFileMetaData();
      } else if (this.tabActiveName === "relateTest") {
        this.$nextTick(() => {
          this.getRelatedTest();
        });
      }
    },
    caseId() {
      getRelationshipCountCase(this.caseId).then((r) => {
        this.relationshipCount = r.data;
      });
    },
    demandValue() {
      if (this.demandValue.length > 0) {
        this.form.demandId = this.demandValue[this.demandValue.length - 1];
      } else {
        this.form.demandId = null;
      }
    },
  },
  methods: {
    updateRemark(text) {
      this.form.remark = text;
    },
    openComment() {
      this.$emit("openComment");
    },
    getComments(testCase) {
      this.$emit("getComments", testCase);
    },
    setRelationshipCount(count) {
      this.relationshipCount = count;
    },
    setRelationshipGraph(val) {
      this.$emit("syncRelationGraphOpen", val);
    },
    reset() {
      this.tabActiveName = "remark";
    },
    fileValidator(file) {
      return file.size < 500 * 1024 * 1024;
    },
    beforeUpload(file) {
      if (!this.fileValidator(file)) {
        this.$error(this.$t("load_test.file_size_out_of_bounds") + file.name);
        return false;
      }

      if (this.tableData.filter((f) => f.name === file.name).length > 0) {
        this.$error(this.$t("load_test.delete_file") + ", name: " + file.name);
        return false;
      }
    },
    handleUpload(e) {
      // 表格生成上传文件数据
      let file = e.file;
      let user = JSON.parse(localStorage.getItem(TokenKey));
      this.tableData.push({
        name: file.name,
        size: byteToSize(file.size),
        updateTime: new Date().getTime(),
        progress: this.type === "add" ? 100 : 0,
        status: this.type === "add" ? "toUpload" : 0,
        creator: user.name,
        type: getTypeByFileName(file.name),
        isLocal: true,
      });

      if (this.type === "add") {
        // 新增上传
        this.uploadFiles.push(file);
        return false;
      }
      // 上传文件
      this.uploadFile(e, (param) => {
        this.showProgress(e.file, param);
      });
    },
    async uploadFile(param, progressCallback) {
      let progress = 0;
      let file = param.file;
      let data = { belongId: this.caseId, belongType: "testcase" };
      let CancelToken = axios.CancelToken;
      let self = this;

      uploadIssueAttachment(
        file,
        data,
        CancelToken,
        self.cancelFileToken,
        progressCallback
      )
        .then((response) => {
          // 成功回调
          progress = 100;
          param.onSuccess(response);
          progressCallback({ progress, status: "success" });
          self.cancelFileToken.forEach((token, index, array) => {
            if (token.name == file.name) {
              array.splice(token, 1);
            }
          });
        })
        .catch(({ error }) => {
          // 失败回调
          progress = 100;
          progressCallback({ progress, status: "error" });
          self.cancelFileToken.forEach((token, index, array) => {
            if (token.name == file.name) {
              array.splice(token, 1);
            }
          });
        });
    },
    showProgress(file, params) {
      const { progress, status } = params;
      const arr = [...this.tableData].map((item) => {
        if (item.name === file.name) {
          item.progress = progress;
          item.status = status;
        }
        return item;
      });
      this.tableData = [...arr];
    },
    handleExceed(files, fileList) {
      this.$error(this.$t("load_test.file_size_limit"));
    },
    handleSuccess(response, file, fileList) {
      let readyFiles = fileList.filter((item) => item.status === "ready");
      if (readyFiles.length === 0) {
        this.getFileMetaData();
      }
    },
    handleError(err, file, fileList) {
      let readyFiles = fileList.filter((item) => item.status === "ready");
      if (readyFiles.length === 0) {
        this.getFileMetaData();
      }
    },
    handleDelete(file, index) {
      this.$alert(
        (this.cancelFileToken.length > 0
          ? this.$t("load_test.delete_file_when_uploading") + "<br/>"
          : "") +
          this.$t("load_test.delete_file_confirm") +
          file.name +
          "?",
        "",
        {
          confirmButtonText: this.$t("commons.confirm"),
          dangerouslyUseHTMLString: true,
          callback: (action) => {
            if (action === "confirm") {
              this._handleDelete(file, index);
            }
          },
        }
      );
    },
    _handleDelete(file, index) {
      // 中断所有正在上传的文件
      if (this.cancelFileToken && this.cancelFileToken.length >= 1) {
        this.cancelFileToken.forEach((cacelToken) => {
          cacelToken.cancelFunc();
        });
      }
      this.fileList.splice(index, 1);
      this.tableData.splice(index, 1);
      if (this.type === "add") {
        this.uploadFiles.splice(index, 1);
      } else {
        deleteTestCaseAttachment(file.id).then(() => {
          this.$success(this.$t("commons.delete_success"));
          this.getFileMetaData();
        });
      }
    },
    handleUnRelate(file, index) {
      // 取消关联
      this.$alert(
        this.$t("load_test.unrelated_file_confirm") + file.name + "?",
        "",
        {
          confirmButtonText: this.$t("commons.confirm"),
          dangerouslyUseHTMLString: true,
          callback: (action) => {
            if (action === "confirm") {
              this.result.loading = true;
              let unRelateFileIndex = this.tableData.findIndex(
                (f) => f.name === file.name
              );
              this.tableData.splice(unRelateFileIndex, 1);
              if (file.status === "toRelate") {
                // 待关联的记录, 直接移除
                let unRelateId = this.relateFiles.findIndex(
                  (f) => f === file.id
                );
                this.relateFiles.splice(unRelateId, 1);
                this.result.loading = false;
              } else {
                // 已经关联的记录
                this.unRelateFiles.push(file.id);
                let data = {
                  belongType: "testcase",
                  belongId: this.caseId,
                  metadataRefIds: this.unRelateFiles,
                };
                unrelatedAttachment(data).then(() => {
                  this.$success(this.$t("commons.unrelated_success"));
                  this.result.loading = false;
                  this.getFileMetaData(this.issueId);
                });
              }
            }
          },
        }
      );
    },
    handleDump(file) {
      this.$refs.module.init();
      this.dumpFile = file;
    },
    handleCancel(file, index) {
      this.fileList.splice(index, 1);
      let cancelToken = this.cancelFileToken.filter(
        (f) => f.name === file.name
      )[0];
      cancelToken.cancelFunc();
      let cancelFile = this.tableData.filter((f) => f.name === file.name)[0];
      cancelFile.progress = 100;
      cancelFile.status = "error";
    },
    getFileMetaData(id) {
      this.$emit("update:isClickAttachmentTab", true);
      // 保存用例后传入用例id，刷新文件列表，可以预览和下载
      this.fileList = [];
      this.tableData = [];
      let testCaseId;
      if (this.isCopy) {
        testCaseId = this.copyCaseId;
      } else {
        testCaseId = id ? id : this.caseId;
      }
      if (testCaseId) {
        let data = { belongType: "testcase", belongId: testCaseId };
        this.result.loading = true;
        attachmentList(data).then((response) => {
          this.result.loading = false;
          let files = response.data;
          if (!files) {
            return;
          }
          // deep copy
          this.fileList = JSON.parse(JSON.stringify(files));
          this.tableData = JSON.parse(JSON.stringify(files));
          this.tableData.map((f) => {
            f.size = byteToSize(f.size);
            f.status = "success";
            f.progress = 100;
          });
        });
      }
    },
    associationFile() {
      this.$refs.metadataList.open();
    },
    checkRows(rows) {
      let repeatRecord = false;
      for (let row of rows) {
        let rowIndex = this.tableData.findIndex(
          (item) => item.name === row.name
        );
        if (rowIndex >= 0) {
          this.$error(
            this.$t("load_test.exist_related_file") + ": " + row.name
          );
          repeatRecord = true;
          break;
        }
      }
      if (!repeatRecord) {
        if (this.type === "add") {
          // 新增
          rows.forEach((row) => {
            this.relateFiles.push(row.id);
            this.tableData.push({
              id: row.id,
              name: row.name,
              size: byteToSize(row.size),
              updateTime: row.createTime,
              progress: 100,
              status: "toRelate",
              creator: row.createUser,
              type: row.type,
              isLocal: false,
            });
          });
        } else {
          // 编辑
          let metadataRefIds = [];
          rows.forEach((row) => metadataRefIds.push(row.id));
          let data = {
            belongType: "testcase",
            belongId: this.caseId,
            metadataRefIds: metadataRefIds,
          };
          this.result.loading = true;
          relatedAttachment(data).then(() => {
            this.$success(this.$t("commons.relate_success"));
            this.result.loading = false;
            this.getFileMetaData();
          });
        }
      }
    },
    setModuleId(moduleId) {
      let data = {
        id: getUUID(),
        resourceId: getCurrentProjectID(),
        moduleId: moduleId,
        projectId: getCurrentProjectID(),
        fileName: this.dumpFile.name,
        attachmentId: this.dumpFile.id,
      };
      dumpAttachment(data).then(() => {
        this.$success(this.$t("organization.integration.successful_operation"));
      });
    },
    getRelatedTest() {
      this.$refs.relateTest.initTable();
    },
    visibleChange(flag) {
      if (flag) {
        this.getDemandOptions();
      }
    },
    getDemandOptions() {
      this.result = { loading: true };
      this.demandLabel = "";
      issueDemandList(this.projectId)
        .then((r) => {
          this.demandOptions = [];
          if (r.data && r.data.length > 0) {
            this.buildDemandCascaderOptions(r.data, this.demandOptions, []);
          }
          this.addOtherOption();
        })
        .catch(() => {
          this.addOtherOption();
        });
    },
    addOtherOption() {
      this.demandOptions.unshift({
        value: "other",
        label: "Other: " + this.$t("test_track.case.other"),
        platform: "Other",
      });
      if (this.form.demandId === "other") {
        this.demandValue = ["other"];
        this.demandLabel = "Other: " + this.$t("test_track.case.other");
      }
      this.result = { loading: false };
    },
    buildDemandCascaderOptions(data, options, pathArray) {
      this.demandValue = [];
      data.forEach((item) => {
        let option = {
          label: item.platform + ": " + item.name,
          value: item.id,
        };
        if (this.form.demandId === item.id) {
          this.demandLabel = option.label;
        }
        options.push(option);
        pathArray.push(item.id);
        if (item.id === this.form.demandId) {
          this.demandValue = [...pathArray]; // 回显级联选项
        }
        if (item.children && item.children.length > 0) {
          option.children = [];
          this.buildDemandCascaderOptions(
            item.children,
            option.children,
            pathArray
          );
        }
        pathArray.pop();
      });
    },
    filterDemand(node, keyword) {
      if (
        keyword &&
        node.text.toLowerCase().indexOf(keyword.toLowerCase()) !== -1
      ) {
        return true;
      }
      return false;
    },
  },
};
</script>

<style scoped>
.other-info-tabs :deep(.el-tabs__content) {
  padding: 20px 0px;
}

.remark-item {
  padding: 0px 3px;
}

.el-cascader :deep(.el-input) {
  cursor: pointer;
}

.ms-header {
  background: #783887;
  color: white;
  height: 18px;
  width: 18px;
  font-size: xx-small;
  border-radius: 50%;
}

.demand-span {
  display: inline-block;
  max-width: 400px;
  white-space: nowrap;
  text-overflow: ellipsis;
  overflow: hidden;
  word-break: break-all;
  margin-right: 5px;
}

.demandInput {
  width: 200px;
}

.el-icon-plus {
  font-size: 16px;
}

.upload-default {
  background-color: #fbfdff;
  border: 1px dashed #c0ccda;
  border-radius: 6px;
  -webkit-box-sizing: border-box;
  box-sizing: border-box;
  width: 40px;
  height: 30px;
  line-height: 32px;
  vertical-align: top;
  text-align: center;
  cursor: pointer;
  display: inline-block;
}

.upload-default i {
  color: #8c939d;
}

.upload-default:hover {
  border: 1px dashed #783887;
}

.testplan-local-upload-tip {
  display: inline-block;
  position: relative;
  left: 25px;
  top: -5px;
}

.not-testplan-local-upload-tip {
  display: inline-block;
  position: relative;
  left: 25px;
  top: 8px;
}
</style>

<style scoped lang="scss">
@import "@/business/style/index.scss";
.case-edit-wrap {
  :deep(.el-form-item__content) {
    line-height: px2rem(32);
  }
  .case-edit-box {
    width: px2rem(1328);
    min-height: px2rem(1001);
    /* margin-left: px2rem(34); */
    background-color: #fff;
    .edit-header-container {
      height: px2rem(56);
      width: 100%;
      border-bottom: 1px solid rgba(31, 35, 41, 0.15);
      display: flex;
      align-items: center;
      .header-content-row {
        display: flex;
        .back {
          margin-left: px2rem(24);
          width: px2rem(20);
          height: px2rem(20);
          img {
            width: 100%;
            height: 100%;
          }
        }

        .case-name {
          min-width: px2rem(64);
          height: px2rem(24);
          font-size: 16px;
          font-family: "PingFang SC";
          font-style: normal;
          font-weight: 500;
          line-height: px2rem(24);
          color: #1f2329;
          order: 1;
          flex-grow: 0;
          margin-left: px2rem(8);
        }

        .case-edit {
          .case-level {
          }

          .case-version {
            .version-icon {
            }

            .version-title {
            }
          }
        }
      }
    }

    .edit-content-container {
      width: 100%;
      height: 100%;
      display: flex;
      background-color: #fff;
      .required-item:after {
        content: "*";
        color: #f54a45;
        margin-left: px2rem(4);
        width: px2rem(8);
        height: 22px;
        font-weight: 400;
        font-size: 14px;
        line-height: 22px;
      }
      .content-body-wrap {
        // 1024 减去左右padding 各24 和 1px右边框
        width: px2rem(975);
        height: 100%;
        .case-title-wrap {
          display: flex;
          margin-top: px2rem(24);
          margin-bottom: px2rem(8);
          .title-wrap {
            height: 22px;
            font-family: "PingFang SC";
            font-style: normal;
            font-weight: 500;
            font-size: 14px;
            line-height: 22px;
            color: #1f2329;
          }
        }
        .content-conatiner {
          padding-left: px2rem(24);
          padding-right: px2rem(24);
        }
        .comment-common {
          position: fixed;
          bottom: 0px;
          width: px2rem(975);
          z-index: 9999;
        }
        //公共样式
        .content-wrap {
          :deep(.v-note-op) {
            background-color: #f8f9fa !important;
            border-bottom: 1px solid #bbbfc4;
          }
          :deep(.v-note-wrapper) {
            box-sizing: border-box;
            border: 1px solid #bbbfc4 !important;
            border-radius: 4px;
            box-shadow: none !important;
          }
          :deep(.v-note-show) {
            min-height: 65px;
          }
          :deep(.v-left-item) {
            flex: none !important;
          }
        }

        .case-name-row {
          .content-wrap {
            display: flex;
            justify-content: center;
            width: 100%;
            .opt-row {
              width: 100%;
              height: 32px;
            }
          }
        }
        .pre-condition-row {
          .content-wrap {
            display: flex;
            justify-content: center;
            width: 100%;
            min-height: 100px;
            .opt-row {
              :deep(.el-form-item) {
                margin: 0;
              }
              width: 100%;
            }
          }
        }
        .remark-row {
          .content-wrap {
            display: flex;
            justify-content: center;
            width: 100%;
            min-height: 100px;
            .opt-row {
              width: 100%;
              :deep(.el-form-item) {
                margin: 0;
              }
            }
          }
        }
        .attachment-row {
          .attachment-name.case-title-wrap {
            .name.title-wrap {
            }
          }

          .content-wrap {
            .opt-btn {
            }

            .opt-tip {
              font-family: "PingFang SC";
              font-style: normal;
              font-weight: 400;
              font-size: 14px;
              line-height: 22px;
              /* identical to box height, or 157% */

              color: #8f959e;
            }
          }
        }
      }

      .content-base-info-wrap {
        width: px2rem(304);
        min-height: px2rem(864);
        border-left: 1px solid rgba(31, 35, 41, 0.15);
        .case-wrap {
          margin-left: px2rem(24);
          margin-top: px2rem(24);
        }
        .case-title-wrap {
          display: flex;
          .title-wrap {
            font-weight: 500;
            height: 22px;
            font-size: 14px;
            line-height: 22px;
            color: #1f2329;
          }
          margin-bottom: px2rem(8);
        }
        .side-content {
          width: px2rem(256);
          height: 32px;
          :deep(.el-select) {
            width: 100%;
          }
        }
      }
    }

    .edit-footer-container {
      display: flex;
      width: 100%;
      height: px2rem(80);
      background: #ffffff;
      box-shadow: 0px -1px 4px rgba(31, 35, 41, 0.1);
      align-items: center;
      font-family: "PingFang SC";
      font-style: normal;
      font-weight: 400;
      font-size: 14px;
      line-height: 22px;
      text-align: center;
      // 底部按钮激活样式
      .opt-active-primary {
        background: #783887;
        color: #ffffff;
      }
      .opt-disable-primary {
        background: #bbbfc4;
        color: #ffffff;
      }
      .opt-active {
        background: #ffffff;
        color: #1f2329;
      }
      .opt-disable {
        background: #ffffff;
        color: #bbbfc4;
      }

      .save-btn-row {
        margin-left: px2rem(24);
        el-button {
        }
      }

      .save-add-pub-row {
        margin-left: px2rem(12);
        el-button {
        }
      }

      .save-create-row {
        margin-left: px2rem(12);
        el-button {
        }
      }
    }
  }
}
</style>
<style>
.attachment-popover {
  padding: 0 !important;
  height: 80px;
  min-width: 120px !important;
}
</style>
