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
    `power` integer not null,
    `bypass_enabled` boolean not null,
    `chat_channel` varchar(16) null
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
    `interaction_status` varchar(32),
    foreign key(`player_id`) references `mf_player`(`id`) on delete cascade
);

create table `mf_gate`(
    `id` varchar(36) primary key not null,
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
    `status` varchar(32) not null,
    foreign key(`faction_id`) references `mf_faction`(`id`)
);

create table `mf_gate_creation_context`(
    `player_id` varchar(36) primary key not null,
    `version` integer not null,
    `world_id` varchar(36) null,
    `x_1` integer null,
    `y_1` integer null,
    `z_1` integer null,
    `x_2` integer null,
    `y_2` integer null,
    `z_2` integer null,
    `trigger_x` integer null,
    `trigger_y` integer null,
    `trigger_z` integer null,
    foreign key(`player_id`) references `mf_player`(`id`) on delete cascade
);

create table `mf_locked_block`(
    `id` varchar(36) primary key not null,
    `version` integer not null,
    `world_id` varchar(36) not null,
    `x` integer not null,
    `y` integer not null,
    `z` integer not null,
    `chunk_x` integer not null,
    `chunk_z` integer not null,
    `player_id` varchar(36) not null,
    foreign key(`world_id`, `chunk_x`, `chunk_z`) references `mf_claimed_chunk`(`world_id`, `x`, `z`) on delete cascade,
    foreign key(`player_id`) references `mf_player`(`id`) on delete cascade
);

create table `mf_locked_block_accessor`(
    `locked_block_id` varchar(36) not null,
    `player_id` varchar(36) not null,
    primary key(`locked_block_id`, `player_id`),
    foreign key(`locked_block_id`) references `mf_locked_block`(`id`) on delete cascade,
    foreign key(`player_id`) references `mf_player`(`id`) on delete cascade
);

-- No foreign keys here as we want to keep chat channel messages even if factions or players are deleted
-- The reference will be lost but we will still retain the data.
-- Maybe in the future we should soft-delete factions & players instead of deleting them.
create table `mf_chat_channel_message`(
    `timestamp` datetime not null,
    `player_id` varchar(36) not null,
    `faction_id` varchar(36) not null,
    `chat_channel` varchar(16) not null,
    `message` varchar(1024) not null
);

create table `mf_duel`(
    `id` varchar(36) primary key not null,
    `version` integer not null,
    `challenger_id` varchar(36) not null,
    `challenged_id` varchar(36) not null,
    `challenger_health` double not null,
    `challenged_health` double not null,
    `end_time` datetime not null,
    foreign key(`challenger_id`) references `mf_player`(`id`) on delete cascade,
    foreign key(`challenged_id`) references `mf_player`(`id`) on delete cascade
);

create table `mf_duel_invite`(
    `inviter_id` varchar(36) not null,
    `invitee_id` varchar(36) not null,
    primary key(`inviter_id`, `invitee_id`)
);