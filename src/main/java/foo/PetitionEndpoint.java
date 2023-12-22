package foo;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiMethod.HttpMethod;
import com.google.api.server.spi.config.ApiNamespace;
import com.google.api.server.spi.config.Named;
import com.google.api.server.spi.config.Nullable;
import com.google.api.server.spi.response.UnauthorizedException;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query.CompositeFilterOperator;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.FilterPredicate;
import com.google.appengine.api.datastore.Query.SortDirection;

@Api(name = "petitionApi",
        version = "v1",
        audiences = "63622819491-158fpon6hetnglfhnefvm2fkd27521cu.apps.googleusercontent.com",
        clientIds = {"63622819491-158fpon6hetnglfhnefvm2fkd27521cu.apps.googleusercontent.com"},
        namespace =
        @ApiNamespace(
                ownerDomain = "",
                ownerName = "",
                packagePath = "")
)
public class PetitionEndpoint {

    private static final DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();

    private void validateUser(String user) throws UnauthorizedException {
        if (user == null || "null".equals(user)) {
            throw new UnauthorizedException("Invalid credentials");
        }
    }

    private boolean petitionTitleExists(String title) {
        Query query = new Query("Petition").setFilter(new FilterPredicate("title", FilterOperator.EQUAL, title));
        return datastore.prepare(query).countEntities(FetchOptions.Builder.withLimit(1)) > 0;
    }

    private boolean hasUserSignedPetition(String user, String title) {
        Query query = new Query("Petitioner")
                .setFilter(CompositeFilterOperator.and(
                        FilterOperator.EQUAL.of("petitioner", user), 
                        FilterOperator.EQUAL.of("petition", title)
                ));
        return datastore.prepare(query).countEntities(FetchOptions.Builder.withLimit(1)) > 0;
    }

    @ApiMethod(name = "createPetition", httpMethod = HttpMethod.GET)
    public Entity createPetition(@Named("user") String user, @Named("title") String title, @Named("tag") String tag) throws UnauthorizedException {
        validateUser(user);
        
        if (petitionTitleExists(title)) {
            throw new UnauthorizedException("This Petition title already exist.");
        }

        Entity petition = new Entity("Petition");
        petition.setProperty("title", title);
        petition.setProperty("author", user);
        petition.setProperty("date", LocalDate.now().toString());
        petition.setProperty("tags", tag);

        datastore.put(petition);

        return petition;
    }

    /* Endpoint to sign petition */
    @ApiMethod(name = "signPetition", httpMethod = HttpMethod.GET)
    public Entity signPetition(@Named("user") String user, @Named("title") String title) throws UnauthorizedException {
        validateUser(user);

        if (!petitionTitleExists(title)) {
            throw new UnauthorizedException("This petition does not exist.");
        }

        if (hasUserSignedPetition(user, title)) {
            throw new UnauthorizedException("This user has already signed this petition.");
        }

        Entity petitioner = new Entity("Petitioner");
        petitioner.setProperty("petitioner", user);
        petitioner.setProperty("petition", title);
        petitioner.setProperty("date", LocalDate.now().toString());

        datastore.put(petitioner);

        return petitioner;
    }

    /* List top 100 petitions */
    @ApiMethod(name = "topPetitions", httpMethod = HttpMethod.GET)
    public List<Entity> topPetitions(@Nullable @Named("next") String cursorString) {
        Query q = new Query("Petition").addSort("date", SortDirection.DESCENDING);

		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
		PreparedQuery pq = datastore.prepare(q);
		List<Entity> result = pq.asList(FetchOptions.Builder.withLimit(100));
		
        return result;
    }
}
