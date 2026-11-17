package fr.isep.campuspass

import android.content.Context
import fr.isep.campuspass.domain.Event
import fr.isep.campuspass.domain.EventCategory
import fr.isep.campuspass.payment.GooglePayClient
import fr.isep.campuspass.repository.AuthRepository
import fr.isep.campuspass.repository.EventRepository
import fr.isep.campuspass.repository.InMemoryEventRepository
import fr.isep.campuspass.repository.InMemoryTicketingRepository
import fr.isep.campuspass.repository.TicketingRepository
import fr.isep.campuspass.security.TicketSigner
import kotlinx.datetime.LocalDateTime

/** Free Unsplash photos (https://unsplash.com/license), loaded over the network. */
private fun poster(id: String) = "https://images.unsplash.com/photo-$id?w=1000&q=80&auto=format&fit=crop"

/** Manual dependency injection: single in-memory instances shared by all ViewModels. */
object AppContainer {
    val auth = AuthRepository()

    val events: EventRepository = InMemoryEventRepository(
        listOf(
            Event(1, "Winter Gala 2026", "The most elegant night of the year: dinner, live band and dance floor until 3 am. Dress code: black tie.", EventCategory.PARTY, "Pavillon Dauphine, Paris 16e", LocalDateTime(2026, 12, 12, 20, 0), 3500, 300, poster("1714972383523-7c636d2f0e9b"), "BDE ISEP"),
            Event(2, "Thursday Afterwork", "Start the weekend early with the whole campus. Soft drinks and snacks included.", EventCategory.PARTY, "Le Bar du Campus, Issy", LocalDateTime(2026, 11, 19, 19, 0), 800, 120, poster("1627020730793-2ccb5cd55e99"), "BDE ISEP"),
            Event(3, "ISEP Live Concert", "Three student bands, one stage, a lot of noise. Rock, electro and jazz fusion.", EventCategory.CONCERT, "Amphi Notre-Dame-des-Champs", LocalDateTime(2026, 12, 5, 20, 30), 1500, 250, poster("1470229722913-7c0e2dbbafd3"), "ISEP Music Club"),
            Event(4, "Inter-School Basketball Cup", "ISEP vs. the best engineering schools of Paris. Come and cheer for the team!", EventCategory.SPORT, "Gymnase Jean Bouin, Issy", LocalDateTime(2026, 11, 28, 14, 0), 500, 120, poster("1504450758481-7338eba7524a"), "BDS ISEP"),
            Event(5, "Tech Talk: Kotlin Multiplatform in Production", "Engineers from a French scale-up share how they ship one Kotlin codebase to Android and iOS.", EventCategory.TALK, "Amphi Issy-les-Moulineaux", LocalDateTime(2026, 11, 25, 18, 30), 300, 150, poster("1540575467063-178a50c2df87"), "ISEP Dev Club"),
            Event(6, "24h Hackathon", "24 hours to build an app that improves campus life. Food, mentors and prizes.", EventCategory.TALK, "ISEP Lab, Issy", LocalDateTime(2026, 12, 1, 9, 0), 1000, 80, poster("1504384308090-c894fdcc538d"), "ISEP Dev Club"),
            Event(7, "Drama Club: Le Malade imaginaire", "Molière's classic, revisited by the ISEP Drama Club. Surtitles in English.", EventCategory.CULTURE, "Théâtre de la Cité, Paris 14e", LocalDateTime(2026, 12, 10, 20, 0), 600, 90, poster("1503095396549-807759245b35"), "ISEP Drama Club"),
            Event(8, "Cinema Night: Sci-Fi Classics", "Double feature on the big screen, popcorn included.", EventCategory.CULTURE, "Amphi Notre-Dame-des-Champs", LocalDateTime(2026, 11, 26, 19, 30), 400, 60, poster("1478720568477-152d9b164e26"), "ISEP Ciné Club"),
            Event(9, "Ski Trip: Les 2 Alpes", "A week at 3,600 m: transport, chalet, ski pass and après-ski parties.", EventCategory.TRIP, "Les 2 Alpes, Isère", LocalDateTime(2027, 1, 23, 7, 0), 28900, 40, poster("1707080193970-b24078d96417"), "BDS ISEP"),
        ),
    )

    /** In production the signing secret stays on the server. Here it is in memory for the demo. */
    private val signer = TicketSigner("campuspass-demo-secret-2026")

    val ticketing: TicketingRepository = InMemoryTicketingRepository(
        events = events,
        signer = signer,
        alreadySold = mapOf(1 to 214, 2 to 118, 3 to 151, 4 to 38, 5 to 61, 6 to 80, 7 to 47, 8 to 22, 9 to 33),
    )

    lateinit var googlePay: GooglePayClient
        private set

    fun init(context: Context) {
        googlePay = GooglePayClient(context.applicationContext)
    }
}
