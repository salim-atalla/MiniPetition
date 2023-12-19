
import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiNamespace;
import com.google.api.server.spi.config.Named;

import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreException;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.IncompleteKey;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.ListValue;
import com.google.cloud.datastore.Query;
import com.google.cloud.datastore.QueryResults;
import com.google.cloud.datastore.StructuredQuery.OrderBy;
import com.google.cloud.datastore.StringValue;
import com.google.cloud.datastore.Value;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Api(name = "petitionApi", version = "v1", audiences = "315994710664-2i0jopet152a0vl8kpluhtmhq5ttjs3t.apps.googleusercontent.com", clientIds = {
        "315994710664-2i0jopet152a0vl8kpluhtmhq5ttjs3t.apps.googleusercontent.com" }, namespace = @ApiNamespace(ownerDomain = "petitionapp.example.com", ownerName = "petitionapp.example.com", packagePath = "v1"))
public class PetitionApi {

    private final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();

    @ApiMethod(name = "createPetition", httpMethod = ApiMethod.HttpMethod.POST)
    public Petition createPetition(@Named("title") String title, @Named("content") String content) {
        try {
            // Create an empty list for signatures
            List<String> signatures = Collections.emptyList();

            // Create a new petition entity in Datastore
            IncompleteKey incompleteKey = datastore.newKeyFactory().setKind("Petition").newKey();
            Key key = datastore.allocateId(incompleteKey);

            Entity petitionEntity = Entity.newBuilder(key)
                    .set("title", title)
                    .set("content", content)
                    .set("timestamp", System.currentTimeMillis())
                    .set("signatures", createListValue(signatures))
                    .build();

            // Save the entity to Datastore
            datastore.put(petitionEntity);

            // Convert the Datastore entity to your Petition class
            return convertEntityToPetition(petitionEntity);
        } catch (DatastoreException e) {
            // Handle the exception, log it, or throw a more specific exception
            throw new RuntimeException("Failed to create petition", e);
        }
    }

    // Helper method to create ListValue from a list of strings
    private ListValue createListValue(List<String> values) {
        return values.stream()
                .map(StringValue::of)
                .collect(Collectors.collectingAndThen(
                        Collectors.toList(),
                        ListValue::of));
    }

    @ApiMethod(name = "signPetition", httpMethod = ApiMethod.HttpMethod.POST)
    public Petition signPetition(@Named("petitionId") Long petitionId, @Named("userId") String userId) {
        try {
            // Fetch the petition entity from Datastore based on petitionId
            Key petitionKey = datastore.newKeyFactory().setKind("Petition").newKey(petitionId);
            Entity petitionEntity = datastore.get(petitionKey);

            if (petitionEntity != null) {
                // Check if the user has already signed the petition
                List<String> signatures = petitionEntity.getList("signatures")
                        .stream()
                        .map(Value::get)
                        .map(value -> value.toString())
                        .collect(Collectors.toList());

                // User has not signed, update the petition entity
                petitionEntity = Entity.newBuilder(petitionEntity)
                        .set("signatures", createListValue(addSignature(signatures, userId)))
                        .build();

                // Save the updated entity to Datastore
                datastore.put(petitionEntity);

                // Convert the Datastore entity to your Petition class
                return convertEntityToPetition(petitionEntity);
            } else {
                // Petition not found
                return null;
            }
        } catch (DatastoreException e) {
            // Handle the exception, log it, or throw a more specific exception
            throw new RuntimeException("Failed to sign petition", e);
        }
    }

    @ApiMethod(name = "getPetitions", httpMethod = ApiMethod.HttpMethod.GET)
    public List<Petition> getPetitions() {
        try {
            // Query all petition entities from Datastore
            Query<Entity> query = Query.newEntityQueryBuilder().setKind("Petition")
                    .setOrderBy(OrderBy.desc("timestamp"))
                    .build();
            QueryResults<Entity> results = datastore.run(query);

            // Convert the Datastore entities to your Petition class
            List<Petition> petitionList = new ArrayList<>();
            while (results.hasNext()) {
                Entity petitionEntity = results.next();
                petitionList.add(convertEntityToPetition(petitionEntity));
            }

            return petitionList;
        } catch (DatastoreException e) {
            // Handle the exception, log it, or throw a more specific exception
            throw new RuntimeException("Failed to get petitions", e);
        }
    }

    // Helper method to convert Datastore entity to Petition class
    private Petition convertEntityToPetition(Entity entity) {
        return new Petition(
                entity.getKey().getId(),
                entity.getString("title"),
                entity.getString("content"),
                entity.getList("signatures")
                        .stream()
                        .map(Value::get)
                        .map(value -> value.toString())
                        .collect(Collectors.toList()),
                entity.getLong("timestamp"));
    }

    // Helper method to add a new signature to the list of signatures
    private List<String> addSignature(List<String> signatures, String userId) {
        if (signatures == null) {
            signatures = new ArrayList<>();
        }
        signatures.add(userId);
        return signatures;
    }
}
