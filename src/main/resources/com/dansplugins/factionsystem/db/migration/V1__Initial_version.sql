create table `mf_faction`(
    `id` varchar(36) primary key not null,
    `version` integer not null,
    `name` varchar(1024) not null,
    `description` varchar(4096) not null,
    `flags` json not null,
    `prefix` varchar(256),
    `home_world_id` varchar(36),
    `home_x` double,
    `home_y` double,
    `home_z` double,
    `home_yaw` real,
    `home_pitch` real,
    `bonus_power` integer not null,
    `autoclaim` boolean not null,
    `roles` json not null,
    `default_role_id` varchar(36) not null,
    `default_permissions` json not null
);

create table `mf_player`(
    `id` varchar(36) primary key not null,
    `version` integer not null,
    `power` integer not null
);

create table `mf_faction_member`(
    `faction_id` varchar(36) not null,
    `player_id` varchar(36) not null,
    `role_id` varchar(36) not null,
    primary key(`faction_id`, `player_id`),
    foreign key(`faction_id`) references `mf_faction`(`id`) on delete cascade,
    foreign key(`player_id`) references `mf_player`(`id`) on delete cascade
);

create table `mf_faction_invite`(
    `faction_id` varchar(36) not null,
    `player_id` varchar(36) not null,
    primary key(`faction_id`, `player_id`),
    foreign key(`faction_id`) references `mf_faction`(`id`) on delete cascade,
    foreign key(`player_id`) references `mf_player`(`id`) on delete cascade
);

create table `mf_faction_chat_member`(
    `faction_id` varchar(36) not null,
    `player_id` varchar(36) not null,
    primary key(`faction_id`, `player_id`),
    foreign key(`faction_id`) references `mf_faction`(`id`) on delete cascade,
    foreign key(`player_id`) references `mf_player`(`id`) on delete cascade
);

create table `mf_law`(
    `id` varchar(36) primary key not null,
    `version` integer not null,
    `faction_id` varchar(36) not null,
    `text` varchar(4096) not null,
    foreign key(`faction_id`) references `mf_faction`(`id`) on delete cascade
);

create table `mf_faction_relationship`(
    `id` varchar(36) primary key not null,
    `faction_id` varchar(36) not null,
    `target_id` varchar(36) not null,
    `type` varchar(32) not null,
    foreign key(`faction_id`) references `mf_faction`(`id`) on delete cascade,
    foreign key(`target_id`) references `mf_faction`(`id`) on delete cascade
);

create table `mf_claimed_chunk`(
    `world_id` varchar(36) not null,
    `x` integer not null,
    `z` integer not null,
    `faction_id` varchar(36) not null,
    primary key(`world_id`, `x`, `z`),
    foreign key(`faction_id`) references `mf_faction`(`id`) on delete cascade
);

create table `mf_player_interaction_status`(
    `player_id` varchar(36) primary key not null,
    `interaction_status` varchar(32) not null,
    foreign key(`player_id`) references `mf_player`(`id`) on delete cascade
);

create table `mf_gate`(
    `gate_id` varchar(36) primary key not null,
    `version` integer not null,
    `faction_id` varchar(36) not null,
    `world_id` varchar(36) not null,
    `min_x` integer not null,
    `min_y` integer not null,
    `min_z` integer not null,
    `max_x` integer not null,
    `max_y` integer not null,
    `max_z` integer not null,
    `trigger_x` integer not null,
    `trigger_y` integer not null,
    `trigger_z` integer not null,
    `material` varchar(32) not null,
    `sound_effect` varchar(32) not null,
    `status` varchar(32) not null,
    foreign key(`faction_id`) references `mf_faction`(`id`)
)