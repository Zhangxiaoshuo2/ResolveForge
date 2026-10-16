-- Synthetic demo data only. NONEXISTENT deliberately has no database row.
INSERT INTO commerce_orders VALUES
 ('RF1001','alice','机械键盘',399.00,'PAID','2026-10-10 10:00:00'),
 ('RF1002','alice','无线鼠标',199.00,'UNSHIPPED','2026-10-11 10:00:00'),
 ('RF1003','alice','显示器',1599.00,'SHIPPED','2026-10-12 10:00:00'),
 ('RF1004','alice','USB 扩展坞',259.00,'DELIVERED','2026-10-09 10:00:00'),
 ('RF2001','bob','降噪耳机',899.00,'DELIVERED','2026-10-08 10:00:00');
INSERT INTO commerce_logistics VALUES
 ('RF1001',NULL,NULL,'NOT_SHIPPED','订单已支付，等待仓库处理','2026-10-10 10:05:00'),
 ('RF1002',NULL,NULL,'NOT_SHIPPED','商品备货中，尚未发货','2026-10-11 10:05:00'),
 ('RF1003','顺丰速运','DEMO-SF1003','IN_TRANSIT','已到达杭州转运中心，正在运输中','2026-10-15 16:00:00'),
 ('RF1004','中通快递','DEMO-ZTO1004','SIGNED','已由收件人签收','2026-10-13 11:00:00'),
 ('RF2001','京东物流','DEMO-JD2001','SIGNED','已由收件人签收','2026-10-12 14:00:00');
