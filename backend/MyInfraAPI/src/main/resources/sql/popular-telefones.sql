USE mydb;

START TRANSACTION;

-- Códigos internacionais de países e territórios.
INSERT INTO DDI (DDI)
SELECT lista.codigo
FROM JSON_TABLE(
    '[
        1, 7, 20, 27, 30, 31, 32, 33, 34, 36, 39, 40, 41,
        43, 44, 45, 46, 47, 48, 49, 51, 52, 53, 54, 55, 56,
        57, 58, 60, 61, 62, 63, 64, 65, 66, 81, 82, 84, 86,
        90, 91, 92, 93, 94, 95, 98, 211, 212, 213, 216, 218, 220,
        221, 222, 223, 224, 225, 226, 227, 228, 229, 230, 231, 232, 233,
        234, 235, 236, 237, 238, 239, 240, 241, 242, 243, 244, 245, 246,
        247, 248, 249, 250, 251, 252, 253, 254, 255, 256, 257, 258, 260,
        261, 262, 263, 264, 265, 266, 267, 268, 269, 290, 291, 297, 298,
        299, 350, 351, 352, 353, 354, 355, 356, 357, 358, 359, 370, 371,
        372, 373, 374, 375, 376, 377, 378, 380, 381, 382, 383, 385, 386,
        387, 389, 420, 421, 423, 500, 501, 502, 503, 504, 505, 506, 507,
        508, 509, 590, 591, 592, 593, 594, 595, 596, 597, 598, 599, 670,
        672, 673, 674, 675, 676, 677, 678, 679, 680, 681, 682, 683, 685,
        686, 687, 688, 689, 690, 691, 692, 850, 852, 853, 855, 856, 880,
        886, 960, 961, 962, 963, 964, 965, 966, 967, 968, 970, 971, 972,
        973, 974, 975, 976, 977, 992, 993, 994, 995, 996, 998
    ]',
    '$[*]' COLUMNS (codigo VARCHAR(3) PATH '$')
) AS lista
LEFT JOIN DDI AS existente
    ON CAST(REPLACE(TRIM(existente.DDI), '+', '') AS BINARY)
       = CAST(lista.codigo AS BINARY)
WHERE existente.idDDI IS NULL;

-- Códigos de área do Brasil, vinculados ao ID do DDI 55 no banco local.
INSERT INTO DDD (DDD, DDI_idDDI)
SELECT lista.codigo, brasil.idDDI
FROM JSON_TABLE(
    '[
        11, 12, 13, 14, 15, 16, 17, 18, 19, 21, 22, 24,
        27, 28, 31, 32, 33, 34, 35, 37, 38, 41, 42, 43,
        44, 45, 46, 47, 48, 49, 51, 53, 54, 55, 61, 62,
        63, 64, 65, 66, 67, 68, 69, 71, 73, 74, 75, 77,
        79, 81, 82, 83, 84, 85, 86, 87, 88, 89, 91, 92,
        93, 94, 95, 96, 97, 98, 99
    ]',
    '$[*]' COLUMNS (codigo VARCHAR(2) PATH '$')
) AS lista
CROSS JOIN (
    SELECT MIN(idDDI) AS idDDI
    FROM DDI
    WHERE REPLACE(TRIM(DDI), '+', '') = '55'
) AS brasil
LEFT JOIN DDD AS existente
    ON CAST(existente.DDD AS BINARY) = CAST(lista.codigo AS BINARY)
   AND existente.DDI_idDDI IN (
       SELECT idDDI
       FROM DDI
       WHERE REPLACE(TRIM(DDI), '+', '') = '55'
   )
WHERE brasil.idDDI IS NOT NULL
  AND existente.idDDD IS NULL;

COMMIT;

-- Conferência. Em um banco sem códigos extras, espere 206 e 67.
SELECT COUNT(DISTINCT REPLACE(TRIM(DDI), '+', '')) AS total_ddis
FROM DDI;

SELECT COUNT(DISTINCT d.DDD) AS total_ddds_brasil
FROM DDD AS d
JOIN DDI AS i ON i.idDDI = d.DDI_idDDI
WHERE REPLACE(TRIM(i.DDI), '+', '') = '55';