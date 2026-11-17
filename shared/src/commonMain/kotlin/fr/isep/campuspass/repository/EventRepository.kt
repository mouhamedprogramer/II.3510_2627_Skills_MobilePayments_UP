package fr.isep.campuspass.repository

import fr.isep.campuspass.domain.Event
import fr.isep.campuspass.domain.EventCategory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Single access point to the event catalog. */
interface EventRepository {
    val events: StateFlow<List<Event>>
    fun event(id: Int): Event?

    /** Events matching a keyword (title, venue, description) and/or a category, sorted by date. */
    fun search(query: String? = null, category: EventCategory? = null): List<Event>
}

class InMemoryEventRepository(initial: List<Event>) : EventRepository {
    private val _events = MutableStateFlow(initial.sortedBy { it.startsAt })
    override val events: StateFlow<List<Event>> = _events

    override fun event(id: Int): Event? = _events.value.firstOrNull { it.id == id }

    override fun search(query: String?, category: EventCategory?): List<Event> {
        val q = query?.trim()?.takeIf { it.isNotEmpty() }
        return _events.value.filter { e ->
            (category == null || e.category == category) &&
                (q == null || listOf(e.title, e.venue, e.description, e.organizer).any { it.contains(q, ignoreCase = true) })
        }
    }
}
