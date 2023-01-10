import el from "element-ui/lib/locale/lang/zh-TW";
import fu from "fit2cloud-ui/src/locale/lang/zh-TW"; // 加载fit2cloud的内容
import mf from "metersphere-frontend/src/i18n/lang/zh-TW"

const message = {
  home: {
    table: {
      index: "序號",
      task_type: "任務類型",
      run_rule: "運行規則",
      task_status: "任務狀態",
      next_execution_time: "下次執行時間",
      create_user: "創建人",
      update_time: "更新時間",
    },
    case: {
      index: "排名",
      case_name: "用例名稱",
      case_type: "用例類型",
      test_plan: "所屬測試計劃",
      failure_times: "失敗次數",
    },
    rate: {
      case_review: "評審率",
      case_review_pass: "評審通過率",
      cover: "覆蓋率",
      legacy: "遺留率",
      legacy_issue: "遺留缺陷佔比",
    },
    dashboard: {
      public: {
        this_week: "本週",
        load_error: "加載失敗",
        no_data: "暫無數據",
        no_search_data: '沒有找到相關內容',
      },
      case_finished_review_pass_tip: "已評審通過的案例/所有完成評審的案例*100%"
    },
    case_review_dashboard: {
      case_count: "用例數量",
      not_review: "未評審",
      finished_review: "已評審",
      not_pass: "未通過",
      pass: "已通過",
    },
    relevance_dashboard: {
      api_case: "接口用例",
      scenario_case: "場景用例",
      performance_case: "性能用例",
      relevance_case_count: "關聯用例數量",
      not_cover: "未覆蓋",
      cover: "已覆蓋",
    },
    bug_dashboard: {
      un_closed_bug_count: "遺留缺陷數",
      un_closed_range: "遺留率",
      un_closed_range_tips: "未關閉缺陷/所有關聯的缺陷*100%",
      un_closed_bug_case_range: "遺留缺陷佔比",
      un_closed_bug_case_range_tips: "未關閉缺陷/所有關聯的用例*100%",
      un_closed_count: "遺留缺陷數",
      total_count: "缺陷總數",
      case_count: "用例總數",
    }
  },
  plan: {
    batch_delete_tip: "批量刪除測試計劃，是否繼續？",
  },
  table: {
    all_case_content: "全部用例"
  },
  review: {
    result_distribution: "結果分布",
    review_pass_rule: '評審通過標準',
    review_pass_rule_all: '全部通過',
    review_pass_rule_single: '單人通過',
  },
  case: {
    use_case_detail: "用例詳情",
    associate_test_cases: "關聯測試用例",
    dependencies: "依賴關繫",
    comment: "評論",
    change_record: "變更記錄",
    case_name: "用例名稱",
    please_enter_the_case_name: "請輸入用例名稱",
    preconditions: "前置條件",
    please_enter_preconditions: "請輸入前置條件",
    attachment: "附件",
    none: "暫無",
    commented: "髮佈了評論",
    add_attachment: "添加附件",
    file_size_limit: "支持任意類型文件，文件大小不超過 500MB",
    add_steps: "添加步骤",
    copy_this_step: "複製該步驟",
    more: "更多",
    follow: "關注",
    followed: "已關注",
    add_to_public_case: "添加到公共用例庫",
    added_to_public_case: "已添加到公共用例庫",
    make_comment: "髮表評論",
    please_enter_comment: "請輸入評論",
    associated_defect: "關聯缺陷",
    create_defect: "創建缺陷",
    associate_existing_defects: "關聯現有缺陷",
    search_by_id: "通過ID或名稱蒐索",
    relieve: "解除依賴",
    content_before_change: "變更前內容",
    content_after_change: "變更後內容",
    empty_tip: "空值",
    all_case: "全部用例",
    all_scenes: "全部場景",
    all_api: "全部接口",
    associated_files:"關聯文件",
    empty_file: "暫無文件",
    upload_file: "上傳文件",
    selected: "已選擇",
    strip: "條",
    clear: "清空",
    please_enter_a_text_description: "請輸入文本描述",
    please_enter_expected_results: "請輸入預期結果",
    please_enter_comments: "請輸入備注",
    disassociate: "取消關联",
    saveAndCreate: "保存並新建",
    last_version: "最新版本",
    set_new: "置新",
    version_comparison: "版本對比",
    compare: "對比",
    project: "項目",
    create_version: "新建版本",
    choose_copy_info: "選擇復製信息",
  }
}

export default {
  ...el,
  ...fu,
  ...mf,
  ...message
};
