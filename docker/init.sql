CREATE DATABASE IF NOT EXISTS film_prop_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE film_prop_db;

CREATE TABLE IF NOT EXISTS props (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    prop_code VARCHAR(50) NOT NULL UNIQUE COMMENT '道具编号',
    prop_name VARCHAR(100) NOT NULL COMMENT '道具名称',
    scene_type VARCHAR(50) NOT NULL COMMENT '适用场景：古装、现代',
    material VARCHAR(100) COMMENT '材质',
    specification VARCHAR(200) COMMENT '规格',
    quantity INT DEFAULT 1 COMMENT '数量',
    status VARCHAR(20) DEFAULT 'available' COMMENT '状态：available-可用，in_use-使用中',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_prop_code (prop_code),
    INDEX idx_scene_type (scene_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='道具基础信息表';

CREATE TABLE IF NOT EXISTS crews (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    crew_name VARCHAR(100) NOT NULL UNIQUE COMMENT '剧组名称',
    project_name VARCHAR(200) COMMENT '项目名称',
    director VARCHAR(50) COMMENT '导演',
    genre VARCHAR(50) NOT NULL COMMENT '片种：古装、现代',
    start_date DATE COMMENT '拍摄开始日期',
    end_date DATE COMMENT '拍摄结束日期',
    status VARCHAR(20) DEFAULT 'active' COMMENT '状态：active-拍摄中，completed-已完成',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_crew_name (crew_name),
    INDEX idx_genre (genre),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='剧组信息表';

CREATE TABLE IF NOT EXISTS prop_schedule_bindings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    prop_id BIGINT NOT NULL COMMENT '道具ID',
    crew_id BIGINT NOT NULL COMMENT '剧组ID',
    start_date DATE NOT NULL COMMENT '拍摄开始日期',
    end_date DATE NOT NULL COMMENT '拍摄结束日期',
    status VARCHAR(20) DEFAULT 'active' COMMENT '状态：active-生效中，completed-已完成，cancelled-已取消',
    binding_type VARCHAR(20) DEFAULT 'formal' COMMENT '绑定类型：formal-正式绑定，temporary-临时借用',
    remark VARCHAR(500) COMMENT '备注',
    version BIGINT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (prop_id) REFERENCES props(id) ON DELETE CASCADE,
    FOREIGN KEY (crew_id) REFERENCES crews(id) ON DELETE CASCADE,
    INDEX idx_prop_id (prop_id),
    INDEX idx_crew_id (crew_id),
    INDEX idx_start_date (start_date),
    INDEX idx_end_date (end_date),
    UNIQUE KEY uk_prop_crew_period (prop_id, crew_id, start_date, end_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='道具-剧组-档期绑定表';

CREATE TABLE IF NOT EXISTS occupancy_import_batches (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    file_name VARCHAR(200) NOT NULL COMMENT '上传文件名',
    operator VARCHAR(50) COMMENT '操作人',
    total_rows INT DEFAULT 0 COMMENT '数据行总数',
    passed_rows INT DEFAULT 0 COMMENT '校验通过行数',
    failed_rows INT DEFAULT 0 COMMENT '校验未通过行数',
    written_rows INT DEFAULT 0 COMMENT '已写入行数',
    not_written_rows INT DEFAULT 0 COMMENT '未写入行数',
    status VARCHAR(20) DEFAULT 'validated' COMMENT '状态：validated-已校验待写入，committed-已写入',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '上传校验时间',
    committed_at DATETIME COMMENT '写入完成时间',
    INDEX idx_status (status),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='道具占用批量导入批次表';

CREATE TABLE IF NOT EXISTS occupancy_import_rows (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    batch_id BIGINT NOT NULL COMMENT '导入批次ID',
    row_no INT NOT NULL COMMENT '数据行号（不含表头，从1开始）',
    prop_code VARCHAR(50) COMMENT '道具编号（CSV原始值）',
    crew_name VARCHAR(100) COMMENT '剧组名（CSV原始值）',
    start_date_raw VARCHAR(20) COMMENT '开始日期（CSV原始值）',
    end_date_raw VARCHAR(20) COMMENT '结束日期（CSV原始值）',
    remark VARCHAR(500) COMMENT '备注（CSV原始值）',
    prop_id BIGINT COMMENT '解析出的道具ID',
    crew_id BIGINT COMMENT '解析出的剧组ID',
    start_date DATE COMMENT '解析出的开始日期',
    end_date DATE COMMENT '解析出的结束日期',
    validate_status VARCHAR(20) DEFAULT 'failed' COMMENT '校验状态：passed-通过，failed-未通过',
    validate_message VARCHAR(500) COMMENT '校验结论',
    write_status VARCHAR(20) DEFAULT 'pending' COMMENT '写入状态：pending-待写入，written-已写入，not_written-未写入',
    write_message VARCHAR(500) COMMENT '写入结论',
    binding_id BIGINT COMMENT '写入成功后关联的绑定ID',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (batch_id) REFERENCES occupancy_import_batches(id) ON DELETE CASCADE,
    INDEX idx_batch_id (batch_id),
    INDEX idx_write_status (write_status),
    INDEX idx_validate_status (validate_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='道具占用批量导入行表';

CREATE TABLE IF NOT EXISTS schedule_change_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    binding_id BIGINT NOT NULL COMMENT '绑定记录ID',
    prop_id BIGINT NOT NULL COMMENT '道具ID',
    crew_id BIGINT NOT NULL COMMENT '剧组ID',
    original_start_date DATE COMMENT '原开始日期',
    original_end_date DATE COMMENT '原结束日期',
    new_start_date DATE NOT NULL COMMENT '新开始日期',
    new_end_date DATE NOT NULL COMMENT '新结束日期',
    change_type VARCHAR(20) NOT NULL COMMENT '变更类型：create-创建，update-更新，cancel-取消',
    change_reason VARCHAR(500) COMMENT '变更原因',
    operator VARCHAR(50) COMMENT '操作人',
    conflict_detected TINYINT(1) DEFAULT 0 COMMENT '是否检测到冲突：0-无，1-有',
    conflict_description VARCHAR(500) COMMENT '冲突描述',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '变更时间',
    FOREIGN KEY (binding_id) REFERENCES prop_schedule_bindings(id) ON DELETE CASCADE,
    INDEX idx_binding_id (binding_id),
    INDEX idx_prop_id (prop_id),
    INDEX idx_crew_id (crew_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='档期变更流水记录表';
