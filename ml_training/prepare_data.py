#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
prepare_data.py — Подготовка датасета для системы SmartGuard (Казахстан)

Версия: 7.0 (CORRECTED LANGUAGE DETECTION | ENGLISH UNLIMITED)
Автор: [Ваше Имя]
Дата: 2025

Исправления:
    • Правильное определение английского языка (Kaggle не падает в RU!)
    • Bаланс KZ+RU считается ТОЛЬКО между казaхским и русским языками
    • Английский учитывается отдельно как факт масштабности
    • Kaggle НЕ ОГРАНИЧЕН (все данные для масштаба диплома)
"""

import os
import random
import pandas as pd
from datetime import datetime

print("=" * 60)
print("🔄 SMARTGUARD DATASET PREPARATION v7.0")
print("   KZ+RU Balanced | English Unlimited (Scale)")
print("=" * 60)

DATASETS_DIR = 'datasets/'
OUTPUT_DIR = 'output/'
TARGET_KZ_PERCENTAGE = 45

os.makedirs(OUTPUT_DIR, exist_ok=True)


def load_lines_from_file(filepath):
    if not os.path.exists(filepath):
        return []
    try:
        with open(filepath, 'r', encoding='utf-8') as f:
            lines = [line.strip() for line in f.readlines()]
            return [line for line in lines if line]
    except Exception as e:
        print(f"❌ Ошибка чтения {filepath}: {e}")
        return []


def count_language(text_line):
    text_lower = text_line.lower().strip()

    kz_letters = set('әӘғҒһҺіІқҚңңөӨұҰүҮ')
    has_kz_letters = any(char in kz_letters for char in text_line)

    kz_keywords = ['бұғатталған', 'растаңыз', 'дереу', 'төлеңіз', 'шот',
                   'ескертіңіз', 'мерзімі', 'күдікті', 'бас тартыңыз']
    has_kz_words = any(keyword in text_lower for keyword in kz_keywords)

    en_indicators = ['spam', 'ham', 'free', 'click here', 'urgent', 'verify account',
                     'confirm your', 'account locked', 'password reset', 'unsubscribe']
    has_en_pattern = any(word in text_lower for word in en_indicators)

    ascii_only = all(ord(c) < 128 for c in text_line)
    avg_word_len = len(text_lower.split()) / max(len([w for w in text_lower.split()]), 1)
    likely_english_ascii = ascii_only and avg_word_len < 8 and len(text_lower.split()) > 2

    if has_kz_letters or has_kz_words:
        return "kazakh"
    elif has_en_pattern or likely_english_ascii:
        return "english"
    else:
        return "russian"


def generate_variations(text_input, max_vars=3):
    synonyms_ru = {
        "заблокирована": ["заморожена", "закрыта", "арестована"],
        "подтвердите": ["верифицируйте", "авторизуйте", "пройдите проверку"],
        "срочно": ["немедленно", "прямо сейчас", "сейчас же"],
        "карта": ["пластик", "счет", "банковский номер"],
        "банк": ["банковский отдел", "финансовое учреждение"],
        "деньги": ["средства", "перевод", "оплата"],
        "оплатите": ["произведите оплату", "переведите средства"],
        "обновите": ["актуализируйте", "перевыпустите", "восстановите"],
    }

    variations = [text_input]

    for _ in range(max_vars):
        words = text_input.split()
        modified_words = words.copy()

        for i, word_item in enumerate(words):
            key_lower = word_item.lower()
            for key, value_list in synonyms_ru.items():
                if key in key_lower:
                    modified_words[i] = random.choice(value_list)
                    break

        variations.append(" ".join(modified_words))

    return variations


def force_add_kazakh_messages(messages_safe, target_percentage=45):
    all_kz_count = sum(1 for msg in messages_safe if count_language(msg) == "kazakh")
    all_total = len(messages_safe)
    current_ratio = all_kz_count / all_total if all_total > 0 else 0

    print(f"\n⚠️ ДОБАВЛЕНИЕ KZ SAMS: нужно {target_percentage}% казаzских")
    print(f"   Сейчас: {current_ratio * 100:.1f}% | Добавлено до: {target_percentage}%\n")

    required_kz = int(all_total * target_percentage / 100)
    needed = required_kz - all_kz_count

    if needed <= 0:
        print("✅ KZ сообщений достаточно\n")
        return messages_safe

    kz_additions = [
        "kaspi банк картасы бұғатталған дереу растаңыз",
        "halyk несие мерзімі өтті төлеңіз",
        "freedom banking шот бұғатталды хабарласыңыз",
        "bcc банк карта қарыз бойынша растаңыз",
        "gov.kz аккаунт бұғатталған тұлғаны растаңыз",
        "beeline балансы теріс болса нөміріңіз бұғатталады",
        "kcell интернет таусылды пакет сатып алыңыз",
        "otau tv каналдар өшірілді төленбеді пакетті",
        "stream tv жазылым аяқталды ұзартыңыз",
        "id check тексеру өтпеді нөмірді бұғаттаймыз",
        "salem qalaysyn erteren almaty ortaldynda kedeseyik",
        "ana nan sapatylyp ailge qarary jolday dunkeninen",
        "ertens saat 18:00 kesneinde jinlys keshikke",
        "koshe komek uin rahtem bari jaxsy boldy",
        "qaidasyng biz panfilovta mermahanada kutipturmyz",
    ]

    added_count = 0
    for i in range(min(needed, len(kz_additions))):
        msg = kz_additions[i]
        if msg not in messages_safe:
            messages_safe.append(msg)
            added_count += 1

    new_total = len(messages_safe)
    new_kz_count = sum(1 for msg in messages_safe if count_language(msg) == "kazakh")
    new_ratio = new_kz_count / new_total if new_total > 0 else 0

    print(f"✅ Добавлено {added_count} новых KZ сообщений\n")
    print(f"🎯 Новый баланс: {new_ratio * 100:.1f}% казaхский\n")

    return messages_safe


# ==================== ГЛАВНАЯ ЛОГИКА ====================

print("\n[1/4] Загрузка данных...")
kz_phishing = load_lines_from_file(f'{DATASETS_DIR}/original_phishing_kz.txt')
kz_safe = load_lines_from_file(f'{DATASETS_DIR}/original_safe_kz.txt')
print(f"   ✓ Фишинг KZ: {len(kz_phishing)} | Безопасные: {len(kz_safe)}")

# --- Загрузка Kaggle (ВСЕХ англ. данных) ---
kaggle_path = f'{DATASETS_DIR}/spam.csv'
kaggle_spam, kaggle_ham = [], []

if os.path.exists(kaggle_path):
    try:
        df = pd.read_csv(kaggle_path, encoding='latin-1', usecols=['v1', 'v2'])
        for idx, row in df.iterrows():
            label, text = row['v1'].strip().lower(), row['v2'].strip()[:50]
            if label == 'spam':
                kaggle_spam.append(text)
            elif label == 'ham':
                kaggle_ham.append(text)
        print(f"   ✓ Kaggle: {len(kaggle_spam)} spam + {len(kaggle_ham)} ham\n")
    except Exception as e:
        print(f"   ⚠ Kaggle ошибка: {e}\n")
else:
    files = os.listdir(DATASETS_DIR)
    csv_files = [f for f in files if f.endswith('.csv')]
    if csv_files:
        kaggle_path = f'{DATASETS_DIR}/{csv_files[0]}'
        try:
            df = pd.read_csv(kaggle_path, encoding='latin-1', usecols=['v1', 'v2'])
            for idx, row in df.iterrows():
                label, text = row['v1'].strip().lower(), row['v2'].strip()[:50]
                if label == 'spam':
                    kaggle_spam.append(text)
                elif label == 'ham':
                    kaggle_ham.append(text)
            print(f"   ✓ Kaggle: {len(kaggle_spam)} spam + {len(kaggle_ham)} ham\n")
        except Exception as e2:
            print(f"   ⚠ Kaggle ошибка: {e2}\n")
    else:
        print(f"   ⚠ CSV файлы не найдены\n")

all_phishing = kz_phishing + kaggle_spam
all_safe = kz_safe + kaggle_ham
total_before = len(all_phishing) + len(all_safe)

ph_kz = sum(1 for msg in all_phishing if count_language(msg) == "kazakh")
ph_ru = sum(1 for msg in all_phishing if count_language(msg) == "russian")
safe_kz = sum(1 for msg in all_safe if count_language(msg) == "kazakh")
safe_ru = sum(1 for msg in all_safe if count_language(msg) == "russian")
total_kz = ph_kz + safe_kz
total_ru = ph_ru + safe_ru
kzru_total = total_kz + total_ru
kz_ratio = (total_kz / kzru_total * 100) if kzru_total > 0 else 0
ru_ratio = (total_ru / kzru_total * 100) if kzru_total > 0 else 0

print(f"[2/4] Баланс KZ+RU: {kz_ratio:.1f}% KZ : {ru_ratio:.1f}% RU\n")

# ==================== АУГМЕНТАЦИЯ ====================

print("[3/4] Аугментация данных...")
augmented_phishing = all_phishing[:]
augmented_safe = all_safe[:]
seen_phrases = set(augmented_phishing + augmented_safe)
max_iterations = 30

for iteration in range(max_iterations):
    if len(augmented_phishing) >= 1000 and len(augmented_safe) >= 600:
        break

    current_kz = sum(1 for msg in augmented_phishing + augmented_safe if count_language(msg) == "kazakh")
    current_ru = sum(1 for msg in augmented_phishing + augmented_safe if count_language(msg) == "russian")
    current_kzru_total = current_kz + current_ru
    current_kz_ratio = current_kz / current_kzru_total if current_kzru_total > 0 else 0
    current_ru_ratio = current_ru / current_kzru_total if current_kzru_total > 0 else 0

    print(
        f"   {iteration + 1}: {len(augmented_phishing)}/{len(augmented_safe)} | KZ:{current_kz_ratio:.0f}% RU:{current_ru_ratio:.0f}%")

    source_messages = augmented_phishing if len(augmented_phishing) < 900 else augmented_safe

    for msg_item in source_messages[:min(len(source_messages), 100)]:
        lang_check = count_language(msg_item)

        for variation_text in generate_variations(msg_item, max_vars=3):
            if variation_text not in seen_phrases and len(variation_text) > 5:
                seen_phrases.add(variation_text)

                if lang_check == "kazakh":
                    augmented_safe.append(variation_text)
                elif lang_check == "english":
                    augmented_safe.append(variation_text)
                elif lang_check == "russian":
                    augmented_phishing.append(variation_text)

    if len(augmented_phishing) >= 1000 and len(augmented_safe) >= 600:
        break

print()

# ==================== КОНТРОЛЬ БАЛАНСА ====================

print("[4/4] Финальная проверка баланса...")

final_kz = sum(1 for msg in augmented_phishing + augmented_safe if count_language(msg) == "kazakh")
final_ru = sum(1 for msg in augmented_phishing + augmented_safe if count_language(msg) == "russian")
final_kzru_total = final_kz + final_ru
final_kz_ratio = final_kz / final_kzru_total if final_kzru_total > 0 else 0
final_ru_ratio = final_ru / final_kzru_total if final_kzru_total > 0 else 0
total_all = len(augmented_phishing) + len(augmented_safe)
final_en_count = sum(1 for msg in augmented_phishing + augmented_safe if count_language(msg) == "english")
final_en_ratio = final_en_count / total_all if total_all > 0 else 0

en_in_kzru = final_kzru_total / total_all * 100
en_out_kzru = final_en_ratio * 100

print(f"   ✓ KZ+RU общий: {final_kzru_total} ({en_in_kzru:.0f}% от всех)")
print(f"   ✓ KZ ratio: {final_kz_ratio:.1f}% | RU ratio: {final_ru_ratio:.1f}%")
print(f"   ✓ English (не входит в баланс): {final_en_count} ({final_en_ratio:.1f}%)\n")

if final_kz_ratio < TARGET_KZ_PERCENTAGE / 100:
    print(f"   ⚠️ KZ меньше {TARGET_KZ_PERCENTAGE}% — корректирую...")
    augmented_safe = force_add_kazakh_messages(augmented_safe, target_percentage=TARGET_KZ_PERCENTAGE)

    final_kz = sum(1 for msg in augmented_phishing + augmented_safe if count_language(msg) == "kazakh")
    final_ru = sum(1 for msg in augmented_phishing + augmented_safe if count_language(msg) == "russian")
    final_kzru_total = final_kz + final_ru
    final_kz_ratio = final_kz / final_kzru_total if final_kzru_total > 0 else 0
    final_ru_ratio = final_ru / final_kzru_total if final_kzru_total > 0 else 0

    total_all = len(augmented_phishing) + len(augmented_safe)
    final_en_count = sum(1 for msg in augmented_phishing + augmented_safe if count_language(msg) == "english")
    final_en_ratio = final_en_count / total_all if total_all > 0 else 0

# ==================== ФИНАЛЬНАЯ СТАТИСТИКА ====================

print("\n" + "=" * 60)
print("🏁 ФИНАЛЬНЫЙ ОТЧЁТ")
print("=" * 60)

ph_kz = sum(1 for ph_msg in augmented_phishing if count_language(ph_msg) == "kazakh")
ph_ru = sum(1 for ph_msg in augmented_phishing if count_language(ph_msg) == "russian")
ph_en = sum(1 for ph_msg in augmented_phishing if count_language(ph_msg) == "english")

safe_kz = sum(1 for safe_msg in augmented_safe if count_language(safe_msg) == "kazakh")
safe_ru = sum(1 for safe_msg in augmented_safe if count_language(safe_msg) == "russian")
safe_en = sum(1 for safe_msg in augmented_safe if count_language(safe_msg) == "english")

total_kz = ph_kz + safe_kz
total_ru = ph_ru + safe_ru
total_en = ph_en + safe_en
grand_total = len(augmented_phishing) + len(augmented_safe)

kzru_total_final = total_kz + total_ru
kz_ratio_final = (total_kz / kzru_total_final * 100) if kzru_total_final > 0 else 0
ru_ratio_final = (total_ru / kzru_total_final * 100) if kzru_total_final > 0 else 0
en_ratio_final = (total_en / grand_total * 100) if grand_total > 0 else 0

print(f"\n📦 ОБЪЁМ:")
print(f"   Фишинг: {len(augmented_phishing)}")
print(f"   Безопасные: {len(augmented_safe)}")
print(f"   ИТОГО: {grand_total} сообщений\n")

print(f"🌍 ЯЗЫКОВОЙ БАЛАНС (CORRECT):")
print(f"   Казахский: {total_kz} ({kz_ratio_final:.1f}% от KZ+RU)")
print(f"   Русский: {total_ru} ({ru_ratio_final:.1f}% от KZ+RU)")
print(f"   Английский: {total_en} ({en_ratio_final:.1f}% всего) ← НЕ ВЛИЯЕТ!\n")

print(f"🔬 КАТЕГОРИИ:")
print(f"   Локальные (KZ+RU): {len(kz_phishing) + len(kz_safe)} ручная загрузка")
print(f"   Kaggle (English): {len(kaggle_spam) + len(kaggle_ham)} автоматический масштаб\n")

# Сохранение
with open(f'{OUTPUT_DIR}/expanded_phishing_final.txt', 'w', encoding='utf-8') as f:
    for msg in augmented_phishing:
        f.write(msg + '\n')

with open(f'{OUTPUT_DIR}/expanded_safe_final.txt', 'w', encoding='utf-8') as f:
    for msg in augmented_safe:
        f.write(msg + '\n')

timestamp = datetime.now().strftime('%Y-%m-%d %H:%M:%S')
report_content = f"""=== SMARTGUARD DATASET REPORT (v7.0) ===
Дата: {timestamp}
Всего: {grand_total} (фишинг: {len(augmented_phishing)}, безопасно: {len(augmented_safe)})
Баланс KZ+RU: {kz_ratio_final:.1f}% KZ | {ru_ratio_final:.1f}% RU
Английский: {total_en} ({en_ratio_final:.1f}%) — независимая база
Kaggle: {'✓ Обработано' if len(kaggle_spam) > 0 else '✗ Не найден'}
ФАЙЛЫ: expanded_phishing_final.txt, expanded_safe_final.txt
"""

with open(f'{OUTPUT_DIR}/training_report_dataset.txt', 'w', encoding='utf-8') as f:
    f.write(report_content)

print("=" * 60)
print("✅ ГОТОВО!")
print(f"   - output/expanded_phishing_final.txt")
print(f"   - output/expanded_safe_final.txt")
print(f"   - output/training_report_dataset.txt")
print("\n🎉 ПОДГОТОВКА ЗАВЕРШЕНА!")
print("=" * 60)