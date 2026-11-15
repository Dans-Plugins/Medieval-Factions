-- Locked areas (#1393): named cuboids in a faction's territory that only listed players and roles
-- may build or interact in. Additive: three new tables, nothing existing is changed. Areas are
-- removed with their faction; roles live in mf_faction.roles as JSON, so role_id has no foreign key.
create table `mf_locked_area`(
    `id` varchar(36) primary key not null,
    `version` integer not null,
    `faction_id` varchar(36) not null,
    `name` varchar(64) not null,
    `world_id` varchar(36) not null,
    `min_x` integer not null,
    `min_y` integer not null,
    `min_z` integer not null,
    `max_x` integer not null,
    `max_y` integer not null,
    `max_z` integer not null,
    foreign key(`faction_id`) references `mf_faction`(`id`) on delete cascade
);

create table `mf_locked_area_accessor`(
    `locked_area_id` varchar(36) not null,
    `player_id` varchar(36) not null,
    primary key(`locked_area_id`, `player_id`),
    foreign key(`locked_area_id`) references `mf_locked_area`(`id`) on delete cascade,
    foreign key(`player_id`) references `mf_player`(`id`) on delete cascade
);

create table `mf_locked_area_role_accessor`(
    `locked_area_id` varchar(36) not null,
    `role_id` varchar(36) not null,
    primary key(`locked_area_id`, `role_id`),
    foreign key(`locked_area_id`) references `mf_locked_area`(`id`) on delete cascade
);
