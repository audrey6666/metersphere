<template>
  <div class="bar-container">

    <!--  暂时注释  -->
    <!--        <div class="bar-item">-->
    <!--          <img-->
    <!--            src="/assets/module/figma/icon_visible_outlined.svg"/>-->
    <!--          <span>-->
    <!--              流程预览-->
    <!--            </span>-->
    <!--        </div>-->

    <div class="bar-item button-item" @click="passCommentOpen">
      <el-button type="success" size="mini">通过</el-button>
    </div>

    <div class="bar-item button-item un-pass-btn" @click="uhPassCommentOpen">
      <el-button type="danger" plain size="mini">不通过</el-button>
    </div>

    <div class="bar-item click-item"
         @click="commentOpen">
      <img class="icon-item"
           src="/assets/module/figma/icon_add-comment_outlined.svg"/>
      <span>
        评论
      </span>
    </div>

    <div class="bar-item">
      <el-divider direction="vertical"/>
    </div>

    <div class="bar-item click-item"
         :disabled="countNum !== total || countNum >= total"
         @click="handleNext">
      <span>
        下一页
      </span>
      <i class="el-icon-arrow-right"/>
    </div>

    <div class="bar-item click-item"
         :disabled="countNum === total || countNum <= 1"
         @click="handlePre">
      <i class="el-icon-arrow-left"/>
      <span>
        上一页
      </span>
    </div>

    <comment-edit
      :title="commentTitle"
      :tip="commentTip"
      @addComment="addComment"
      ref="commentEdit"/>

  </div>

</template>

<script>

import CommentEdit from "@/business/review/view/components/commnet/CommentEdit";
import {testCaseCommentAdd} from "@/api/test-case-comment";
import {editTestCaseReviewStatus, editTestReviewTestCase} from "@/api/test-review";
import {getCurrentUser} from "@/business/utils/sdk-utils";
export default {
  name: "TestReviewTestCaseEditOperationBar",
  components: {CommentEdit},
  data() {
    return {
      status: null,
      commentTitle: '',
      commentTip: '',
    };
  },
  props: {
    list: {
      type: Array,
      default() {
        return []
      }
    },
    index: {
      type: Number,
      default() {
        return 0
      }
    },
    pageTotal: {
      type: Number,
      default() {
        return 0
      }
    },
    total: {
      type: Number,
      default() {
        return 0
      }
    },
    pageNum: {
      type: Number,
      default() {
        return 0
      }
    },
    pageSize: {
      type: Number,
      default() {
        return 0
      }
    },
    nextPageData: Object,
    prePageData: Object,
    testCase: Object
  },

  computed: {
    countNum() {
      return this.pageSize * (this.pageNum - 1) + this.index + 1;
    }
  },
  methods: {
    handlePre() {
      this.$emit('pre');
    },
    handleNext() {
      this.$emit('next');
    },
    addComment(comment) {
      if (this.status) {
        let param = {};
        param.id = this.testCase.id;
        param.caseId = this.testCase.caseId;
        param.reviewId = this.testCase.reviewId;
        param.comment = comment.description;
        param.status = this.status;
        editTestReviewTestCase(param)
          .then((r) => {
            this.$success(this.$t('commons.save_success'));
            this.$refs.commentEdit.close();
            editTestCaseReviewStatus(this.testCase.reviewId);

            // // 修改当前用例在整个用例列表的状态
            // this.testCases[this.index].status = this.testCase.status;
            this.$emit('refreshTestCaseStatus', r.data);
          })
          .catch(() => {
            this.$refs.commentEdit.close();
          });
      } else {
        comment.caseId = this.testCase.caseId;
        comment.type = 'REVIEW';
        comment.belongId = this.testCase.reviewId;
        comment.author = getCurrentUser().id;
        testCaseCommentAdd(comment)
          .then(() => {
            this.$success(this.$t('test_track.comment.send_success'));
            this.$emit('refreshComment');
            this.$refs.commentEdit.close();
          });
      }
    },
    addCommentOpen(status) {
      this.status = status;
      this.$refs.commentEdit.open();
    },
    uhPassCommentOpen() {
      this.commentTitle = '确定不通过此评论吗';
      this.commentTip = '请输入评审意见(选填)';
      this.addCommentOpen('UnPass');
    },
    passCommentOpen() {
      this.commentTitle = '确定通过此评论吗';
      this.commentTip = '请输入评审意见(选填)';
      this.addCommentOpen('Pass');
    },
    commentOpen() {
      this.commentTitle = '评论';
      this.commentTip = '请输入';
      this.addCommentOpen();
    },
  }
}
</script>

<style scoped>

.bar-container {
  height: 56px;
  width: 100%;
  box-shadow: 0px -1px 4px rgba(31, 35, 41, 0.1);
  background: #FFFFFF;
}

.bar-item {
  float: right;
  display: inline-block;
  line-height: 56px;
  margin-right: 24px;
}

.button-item .el-button {
  width: 80px;
  height: 32px;
}

.icon-item {
  width: 14.67px;
  height: 13px;
}

.un-pass-btn {
  margin-right: 15px;
}

.el-divider {
  width: 1px;
  height: 20px;

  background: #BBBFC4;

  flex: none;
  order: 1;
  flex-grow: 0;
}

.click-item {
  cursor: pointer;
}
</style>
