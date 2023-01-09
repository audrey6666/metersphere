-- init sql
-- 工单名称 v26_create_index
-- 创建人 guoyuqi
ALTER table issues ADD INDEX project_id_index(project_id);

ALTER table issues ADD INDEX creator_index(creator);

ALTER table custom_field ADD INDEX global_index(global);

ALTER table custom_field ADD INDEX scene_index(scene);

ALTER table custom_field ADD INDEX name_index(name);


-- 评审的用例和用户的中间表
CREATE TABLE IF NOT EXISTS test_case_review_test_case_users
(
    case_id   varchar(50) null,
    review_id varchar(50) null,
    user_id   varchar(50) null,
    constraint test_case_review_test_case_users_pk
    unique (case_id, review_id, user_id)
) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_general_ci;
-- 初始化数据
insert into test_case_review_test_case_users
select distinct t1.case_id, t2.review_id, t2.user_id
from test_case_review_test_case t1,
     test_case_review_users t2
where t1.review_id = t2.review_id;



-- 用例评审添加通过标准
ALTER TABLE test_case_review ADD review_pass_rule varchar(20) default 'SINGLE';

-- 评论添加所属的评审ID，或者测试计划ID
ALTER TABLE test_case_comment ADD belong_id varchar(50) NULL;

-- 给历史的评审评论，设置评审ID
INSERT INTO test_case_comment (id, case_id,  description, author, create_time, update_time, status, `type`, belong_id)
SELECT UUID() AS id, tcc.case_id, tcc.description, tcc.author, tcc.create_time, tcc.update_time, tcc.status, tcc.`type`, tcrtc.review_id as belong_id
FROM  test_case_comment tcc
JOIN test_case_review_test_case tcrtc on tcrtc.case_id = tcc.case_id
WHERE tcc.`type` = 'REVIEW';

-- 删除 belong_id 为 NULL 的历史数据
DELETE FROM test_case_comment WHERE  `type` = 'REVIEW' AND belong_id IS NULL;

-- 评论的创建时间添加索引
CREATE INDEX test_case_comment_create_time_IDX USING BTREE ON test_case_comment (create_time);
