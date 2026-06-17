package com.mlo.app.domain

/**
 * Manages built-in profile templates (GTD, FranklinCovey, etc.)
 */
object ProfileTemplateData {

    data class TemplateDefinition(
        val name: String,
        val description: String,
        val category: String,
        val templateJson: String
    )

    /** Simplified GTD workflow template */
    val gtdTemplate = TemplateDefinition(
        name = "GTD (Getting Things Done)",
        description = "Метод Дэвида Аллена: сбор, обработка, организация, обзор, действие",
        category = "PRODUCTIVITY",
        templateJson = """{
          "contexts": [
            {"name":"@phone","label":"Звонки","color":-16711936,"includeIds":[]},
            {"name":"@computer","label":"Компьютер","color":-16776961,"includeIds":[]},
            {"name":"@home","label":"Дома","color":-16711681,"includeIds":[]},
            {"name":"@errands","label":"Поручения","color":-7829368,"includeIds":[]},
            {"name":"@anywhere","label":"Где угодно","color":-14336,"includeIds":[]}
          ],
          "goals": [
            {"name":"WORK","title":"Работа","color":-16776961},
            {"name":"PERSONAL","title":"Личное","color":-16711936}
          ],
          "flags": [
            {"name":"STARRED","label":"Избранное","color":-28928},
            {"name":"WAITING","label":"Ожидание","color":-14336},
            {"name":"DELEGATED","label":"Делегировано","color":-16776961},
            {"name":"SOMEDAY","label":"Когда-нибудь","color":-7829368},
            {"name":"REFERENCE","label":"Справочно","color":-16711681}
          ]
        }"""
    )

    /** FranklinCovey — Big Rocks / Quadrants */
    val franklinCoveyTemplate = TemplateDefinition(
        name = "FranklinCovey (7 навыков)",
        description = "Матрица Эйзенхауэра: важное/срочное, Big Rocks, роли и цели",
        category = "PRODUCTIVITY",
        templateJson = """{
          "contexts": [
            {"name":"Q1","label":"Важно и срочно","color":-65536,"includeIds":[]},
            {"name":"Q2","label":"Важно, не срочно","color":-16711936,"includeIds":[]},
            {"name":"Q3","label":"Срочно, не важно","color":-14336,"includeIds":[]},
            {"name":"Q4","label":"Не важно, не срочно","color":-7829368,"includeIds":[]}
          ],
          "goals": [
            {"name":"FAMILY","title":"Семья","color":-16711936},
            {"name":"CAREER","title":"Карьера","color":-16776961},
            {"name":"HEALTH","title":"Здоровье","color":-16711681},
            {"name":"FINANCE","title":"Финансы","color":-14336},
            {"name":"GROWTH","title":"Развитие","color":-7829368}
          ],
          "flags": [
            {"name":"BIG_ROCK","label":"Big Rock","color":-65536},
            {"name":"QUADRANT_I","label":"Квадрант I","color":-16711936},
            {"name":"QUADRANT_II","label":"Квадрант II","color":-16776961},
            {"name":"DELEGATED","label":"Делегировано","color":-7829368}
          ]
        }"""
    )

    val kanbanTemplate = TemplateDefinition(
        name = "Kanban",
        description = "Визуальная доска: To-Do → Doing → Done, WIP-лимиты",
        category = "PRODUCTIVITY",
        templateJson = """{
          "contexts": [
            {"name":"backlog","label":"Бэклог","color":-7829368,"includeIds":[]},
            {"name":"todo","label":"To-Do","color":-16776961,"includeIds":[]},
            {"name":"doing","label":"В работе","color":-14336,"includeIds":[]},
            {"name":"done","label":"Готово","color":-16711936,"includeIds":[]}
          ],
          "goals": [
            {"name":"PROJECT","title":"Проекты","color":-16776961},
            {"name":"MAINTENANCE","title":"Поддержка","color":-7829368}
          ],
          "flags": [
            {"name":"BLOCKED","label":"Блокировано","color":-65536},
            {"name":"URGENT","label":"Срочно","color":-14336},
            {"name":"WIP","label":"В работе","color":-16711681}
          ]
        }"""
    )

    val allTemplates = listOf(gtdTemplate, franklinCoveyTemplate, kanbanTemplate)
}
