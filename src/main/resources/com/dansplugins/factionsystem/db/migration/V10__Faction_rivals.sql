create table `mf_faction_rival`(
    `faction_id` varchar(36) not null,
    `target_id` varchar(36) not null,
    `created_at` datetime not null,
    primary key(`faction_id`, `target_id`),
    foreign key(`faction_id`) references `mf_faction`(`id`) on delete cascade,
    foreign key(`target_id`) references `mf_faction`(`id`) on delete cascade
);
