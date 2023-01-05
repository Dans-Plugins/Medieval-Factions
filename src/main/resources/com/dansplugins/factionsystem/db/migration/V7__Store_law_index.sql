DELETE FROM `mf_law`;

ALTER TABLE `mf_law` ADD `number` integer NOT NULL;

--CREATE VIEW `lawView` AS SELECT ROW_NUMBER() OVER(PARTITION BY `faction_id`) AS `row_num`, `number` FROM `mf_law` ORDER BY `faction_id`, `row_num`
--UPDATE `lawView` SET `number` = `row_num`
--DROP VIEW `lawView`