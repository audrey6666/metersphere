import el from "element-ui/lib/locale/lang/en";
import fu from "fit2cloud-ui/src/locale/lang/en_US"; // 加载fit2cloud的内容
import mf from "metersphere-frontend/src/i18n/lang/en-US"

const message = {
  home: {
    table: {
      index: "Index",
      task_type: "Task Type",
      run_rule: "Rule",
      task_status: "Status",
      next_execution_time: "Next Execution Time",
      create_user: "Creator",
      update_time: "Update time",
    },
    case: {
      index: "Ranking",
      case_name: "Case Name",
      case_type: "Case Type",
      test_plan: "Test Plan",
      failure_times: "Failure times",
    },
    rate: {
      case_review: "Review rate",
      case_review_pass: "Review pass rate",
      cover: "Cover rate",
      legacy: "Legacy rate",
      legacy_issue: "Percentage of legacy defects",
    },
    dashboard: {
      public: {
        this_week: "Week ",
        load_error: "Loading failure",
        no_data: "No data",
        no_search_data: 'No search data',
      },
      case_finished_review_pass_tip: "Reviewed cases/All reviewed cases *100%"
    },
    case_review_dashboard: {
      case_count: "Case",
      not_review: "Not reviewed",
      finished_review: "Reviewed",
      not_pass: "Not pass",
      pass: "Pass",
    },
    relevance_dashboard: {
      api_case: "Api case",
      scenario_case: "Scenario case",
      performance_case: "Performance case",
      relevance_case_count: "Relevance case",
      not_cover: "Not cover",
      cover: "Cover",
    },
    bug_dashboard: {
      un_closed_bug_count: "Unclosed bug",
      un_closed_range: "Unclosed bug rate",
      un_closed_range_tips: "Unclosed bugs/all associated bugs *100%",
      un_closed_bug_case_range: "Unclosed bug case rate",
      un_closed_bug_case_range_tips: "Unclosed bugs/all associated cases *100%",
      un_closed_count: "Unclosed bug",
      total_count: "All related bug",
      case_count: "Case count",
    }
  },
  plan: {
    batch_delete_tip: "Do you want to continue deleting the test plan?",
  },
  table: {
    all_case_content: "All case"
  },
  review: {
    result_distribution: "Result Distribution",
    review_pass_rule: 'Review Pass Criteria',
    review_pass_rule_all: 'All Pass',
    review_pass_rule_single: 'Single Pass',
  },
  case: {
    use_case_detail: "Use Case Details",
    associate_test_cases: "Associate Test Cases",
    dependencies: "Dependencies",
    comment: "Comment",
    change_record: "Change record",
    case_name: "Case Name",
    please_enter_the_case_name: "Please enter the case name",
    preconditions: "Preconditions",
    please_enter_preconditions: "Please enter preconditions",
    attachment: "Attachment",
    none: "None",
    commented: "Commented",
    add_attachment: "Add Attachment",
    file_size_limit: "Any type of file is supported, and the file size does not exceed 500MB",
    add_steps: "Add Steps",
    copy_this_step: "Copy this step",
    more: "More",
    follow: "Follow",
    followed: "Followed",
    add_to_public_case: "Add to Common Use Case Library",
    added_to_public_case: "Added to public use case library",
    make_comment: "Make comment",
    please_enter_comment: "Please enter a comment",
    associated_defect: "Associated",
    create_defect: "Create defect",
    associate_existing_defects: "Associate existing defects",
    search_by_id: "Search by ID or Name",
    relieve: "Relieve",
    content_before_change: "Content before change",
    content_after_change: "Content after change",
    empty_tip: "Empty",
    all_case: "All case",
    all_scenes: "All scenes",
    all_api: "All interfaces",
    associated_files:"Associated files",
    empty_file: "No files",
    upload_file: "Upload files",
    selected: "Selected",
    strip: "Strip",
    clear: "Clear",
    please_enter_a_text_description: "Please enter a text description",
    please_enter_expected_results: "Please enter expected results",
    please_enter_comments: "Please enter comments",
    disassociate: "Disassociate",
    saveAndCreate: "Save and New",
  }
}
export default {
  ...el,
  ...fu,
  ...mf,
  ...message
};

