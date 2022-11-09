create table `temp_mf_gate`(
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
    foreign key(`faction_id`) references `mf_faction`(`id`) on delete cascade
);

insert into `temp_mf_gate`(
    `id`,
    `version`,
    `faction_id`,
    `world_id`,
    `min_x`,
    `min_y`,
    `min_z`,
    `max_x`,
    `max_y`,
    `max_z`,
    `trigger_x`,
    `trigger_y`,
    `trigger_z`,
    `material`,
    `status`
) select
    `id`,
    `version`,
    `faction_id`,
    `world_id`,
    `min_x`,
    `min_y`,
    `min_z`,
    `max_x`,
    `max_y`,
    `max_z`,
    `trigger_x`,
    `trigger_y`,
    `trigger_z`,
    `material`,
    `status`
from `mf_gate`;

drop table `mf_gate`;

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
    foreign key(`faction_id`) references `mf_faction`(`id`) on delete cascade
);

insert into `mf_gate`(
    `id`,
    `version`,
    `faction_id`,
    `world_id`,
    `min_x`,
    `min_y`,
    `min_z`,
    `max_x`,
    `max_y`,
    `max_z`,
    `trigger_x`,
    `trigger_y`,
    `trigger_z`,
    `material`,
    `status`
) select
    `id`,
    `version`,
    `faction_id`,
    `world_id`,
    `min_x`,
    `min_y`,
    `min_z`,
    `max_x`,
    `max_y`,
    `max_z`,
    `trigger_x`,
    `trigger_y`,
    `trigger_z`,
    `material`,
    `status`
from `temp_mf_gate`;

drop table `temp_mf_gate`;