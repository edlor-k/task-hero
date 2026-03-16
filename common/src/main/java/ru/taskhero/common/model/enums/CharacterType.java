package ru.taskhero.common.model.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Тип персонажа ребёнка.
 * Ребёнок выбирает персонажа при первом входе.
 * Внешний вид персонажа меняется на уровнях: 3, 7, 11, 20, 30, 50, 100.
 */
@Getter
@RequiredArgsConstructor
public enum CharacterType {
    WARRIOR("Воин", "warrior"),
    MAGE("Маг", "mage"),
    ARCHER("Лучник", "archer"),
    HEALER("Целитель", "healer"),
    ROGUE("Разбойник", "rogue"),
    KNIGHT("Рыцарь", "knight");

    /** Отображаемое название. */
    private final String displayName;

    /** Базовое имя файла для изображений. */
    private final String imageBaseName;

    /**
     * Уровни, на которых меняется внешний вид персонажа.
     */
    public static final int[] EVOLUTION_LEVELS = {3, 7, 11, 20, 30, 50, 100};

    /**
     * Получить стадию эволюции (0-7) на основе текущего уровня.
     *
     * @param level текущий уровень
     * @return номер стадии эволюции (0 = начальный вид)
     */
    public static int getEvolutionStage(int level) {
        int stage = 0;
        for (int evolutionLevel : EVOLUTION_LEVELS) {
            if (level >= evolutionLevel) {
                stage++;
            } else {
                break;
            }
        }
        return stage;
    }

    /**
     * Получить путь к изображению персонажа для данного уровня.
     *
     * @param level текущий уровень
     * @return путь к изображению
     */
    public String getImagePath(int level) {
        int stage = getEvolutionStage(level);
        return "/images/characters/" + imageBaseName + "_stage_" + stage + ".png";
    }
}
