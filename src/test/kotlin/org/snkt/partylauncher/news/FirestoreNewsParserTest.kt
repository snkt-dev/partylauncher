package org.snkt.partylauncher.news

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.snkt.partylauncher.news.repository.FirestoreNewsService

class FirestoreNewsParserTest {

    private val service = FirestoreNewsService()

    @Test
    fun testParseFirestoreRestDocuments() {
        val firestoreJson = """
            {
              "documents": [
                {
                  "name": "projects/beachparty/databases/(default)/documents/news/post_1",
                  "fields": {
                    "title": { "stringValue": "Глобальное обновление 2.0" },
                    "text": { "stringValue": "Мы полностью обновили наш модпак и добавили новые механики..." },
                    "image_url": { "stringValue": "https://example.com/banner1.png" },
                    "source_url": { "stringValue": "https://example.com/news/1" },
                    "timestamp": { "timestampValue": "2026-08-24T12:00:00Z" }
                  }
                },
                {
                  "name": "projects/beachparty/databases/(default)/documents/news/post_2",
                  "fields": {
                    "title": { "stringValue": "Итоги строительного конкурса" },
                    "text": { "stringValue": "Поздравляем победителей турнира строителей..." },
                    "imageUrl": { "stringValue": "https://example.com/banner2.png" },
                    "sourceUrl": { "stringValue": "https://example.com/news/2" },
                    "timestamp": { "integerValue": "1787100000000" }
                  }
                }
              ]
            }
        """.trimIndent()

        val news = service.parseNewsResponse(firestoreJson)
        assertEquals(2, news.size)

        val item1 = news.find { it.title == "Глобальное обновление 2.0" }
        assertNotNull(item1)
        assertEquals("https://example.com/banner1.png", item1!!.imageUrl)
        assertEquals("https://example.com/news/1", item1.sourceUrl)
        assertTrue(item1.text.startsWith("Мы полностью обновили"))

        val item2 = news.find { it.title == "Итоги строительного конкурса" }
        assertNotNull(item2)
        assertEquals("https://example.com/banner2.png", item2!!.imageUrl)
        assertEquals("https://example.com/news/2", item2.sourceUrl)
    }

    @Test
    fun testParseFlatJsonArray() {
        val flatJson = """
            [
              {
                "id": "item1",
                "title": "Новый сезон открыт!",
                "text": "Присоединяйтесь к игре прямо сейчас!",
                "image_url": "https://example.com/season.jpg",
                "source_url": "https://example.com/season",
                "timestamp": 1787100000000
              }
            ]
        """.trimIndent()

        val news = service.parseNewsResponse(flatJson)
        assertEquals(1, news.size)
        assertEquals("Новый сезон открыт!", news[0].title)
        assertEquals("https://example.com/season.jpg", news[0].imageUrl)
    }
}
