-- Poziom modelu (Szybki/Standard/Zaawansowany) dla rozmowy AI. Nullable — stare rozmowy = Standard.
ALTER TABLE ai_conversation ADD COLUMN poziom VARCHAR(16) NULL;
