-- Startowa lista FAQ do oferty PDF (przeniesiona z dev). Pomija pytania, które już są — nie dubluje wpisów dodanych ręcznie.
INSERT INTO offer_faq (question, answer, position, active)
SELECT * FROM (
    SELECT 'Czy możemy zobaczyć meble przed zamówieniem całości?' AS question, 'Tak. Wykonujemy pokój wzorcowy, na którym zatwierdzacie materiały, kolory i wygodę łóżek.' AS answer, 0 AS position, b'1' AS active
    UNION ALL SELECT 'Kto montuje meble?', 'Nasza własna ekipa montażowa. Nie korzystamy z podwykonawców.', 1, b'1'
    UNION ALL SELECT 'Jaka jest gwarancja?', 'Udzielamy 24 miesięcy gwarancji, a serwis pogwarancyjny realizujemy w ciągu 14 dni.', 2, b'1'
    UNION ALL SELECT 'Czy możecie dostosować meble do istniejącego projektu architekta?', 'Tak. Pracujemy na dokumentacji architekta i doprecyzowujemy detale produkcyjne.', 3, b'1'
    UNION ALL SELECT 'Jakie są warunki płatności?', 'Standardowo 30% zaliczki po podpisaniu umowy, reszta po montażu kolejnych etapów.', 4, b'1'
    UNION ALL SELECT 'Ile trwa produkcja?', 'Zwykle 8 do 12 tygodni od akceptacji pokoju wzorcowego, zależnie od skali.', 5, b'1'
    UNION ALL SELECT 'Czy montaż może odbywać się przy działającym hotelu?', 'Tak. Montujemy piętrami lub skrzydłami, uzgadniając harmonogram z obsługą hotelu.', 6, b'1'
    UNION ALL SELECT 'Czy zajmujecie się demontażem starych mebli?', 'Tak, na życzenie demontujemy i wywozimy stare wyposażenie.', 7, b'1'
) seed
WHERE NOT EXISTS (SELECT 1 FROM offer_faq f WHERE f.question = seed.question);
