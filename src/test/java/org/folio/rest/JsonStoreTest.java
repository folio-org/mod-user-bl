package org.folio.rest;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.HashMap;
import java.util.Map;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;

import org.junit.Before;
import org.junit.Test;
import org.z3950.zing.cql.CQLParseException;

/**
 *
 * @author kurt
 */

public class JsonStoreTest {
  private JsonStore jsonStore;
  private JsonArray thingList;

  @Before
  public void setup() throws Exception {
    jsonStore = new JsonStore();
    jsonStore.addItem(null, new JsonObject().put("id", "8fa9cdbf-528b-472b-9604-f70560fdacf4").put("name", "thing1"));
    jsonStore.addItem(null, new JsonObject().put("id", "b5197d8e-2c86-4e41-be10-d4562df524dd").put("name", "thing2"));
    jsonStore.addItem(null, new JsonObject().put("id", "f4645809-4f24-4508-a13e-a29d26e012f0").put("name", "thing3"));
    jsonStore.addItem(null, new JsonObject().put("id", "2afb0eb4-5549-47f1-af92-549b6e6005a5").put("name", "thing4"));
    jsonStore.addItem(null, new JsonObject().put("id", "5c1323eb-3ffa-48ea-8e9b-4024dc714cf3").put("name", "thing5"));
    jsonStore.addItem(null, new JsonObject().put("id", "40f50961-2836-433d-a70c-c1251cb0aa9d").put("name", "thing6"));
    jsonStore.addItem(null, new JsonObject().put("id", "83c7a936-87b3-4ac0-b2c9-03cc83c7ab5f").put("name", "thing7"));
    jsonStore.addItem(null, new JsonObject().put("id", "831460ab-3b1e-4c07-a561-78134f38e587").put("name", "thing8"));
    jsonStore.addItem(null, new JsonObject().put("id", "d0f277d1-9be7-4627-b364-f1a9e9d1a5d7").put("name", "thing9"));
    jsonStore.addItem(null, new JsonObject().put("id", "01f3c7d3-05e0-4b5c-b3b7-2534bdff60ec").put("name", "thing10"));

    thingList = new JsonArray()
            .add(new JsonObject()
              .put("color","red")
              .put("species","dog")
              .put("size","small")
              .put("name", "cliff")
            )
            .add(new JsonObject()
              .put("color","blue")
              .put("species","dog")
              .put("size","small")
              .put("name", "fido")
            )
            .add(new JsonObject()
              .put("color","red")
              .put("species","cat")
              .put("size","large")
              .put("name", "jumbo")
            )
            .add(new JsonObject()
              .put("color","black")
              .put("species","cat")
              .put("size","small")
              .put("name", "hector")
            )
            .add(new JsonObject()
              .put("color","black")
              .put("species","cat")
              .put("size","large")
              .put("name", "max")
            );
  }

  @Test
  public void test1() {
    assertNotNull(jsonStore.getCollection(null, null, null));
    assertThat(jsonStore.getCollection(null, null, null).getObjectList(), hasSize(10));
    assertThat(jsonStore.getCollection(null, null, null).getTotalRecords(), is(10));
    assertThat(jsonStore.getCollection(null, null, null).getObjectList().get(0).getString("name"), is("thing1"));
    assertThat(jsonStore.getItem("01f3c7d3-05e0-4b5c-b3b7-2534bdff60ec").getString("name"), is("thing10"));
  }

  @Test
  public void test2() {
    MockCollection jsonList = jsonStore.getCollection(3, 1, null);
    JsonObject ob = jsonList.getObjectList().get(0);
    assertNotNull(ob);
    assertThat(jsonList.getObjectList(), hasSize(1));
    assertThat(jsonList.getObjectList(), hasSize(1));
    assertThat(jsonList.getTotalRecords(), is(1));
    assertTrue(ob.containsKey("name"));
    assertNotNull(ob.getString("name"));

    assertThat(ob.getString("name"), is("thing4"));
  }

  @Test
  public void test3() {
    String id = "83c7a936-87b3-4ac0-b2c9-03cc83c7ab5f";
    assertNotNull(jsonStore.getItem(id));
    jsonStore.deleteItem(id);
    assertNull(jsonStore.getItem(id));
  }

  @Test
  public void test4() throws Exception {
    String id = "5581c6ea-153f-46df-9c94-5a64d371a4f0";
    jsonStore.addItem(null, new JsonObject().put("id", id).put("name", "thing11"));
    JsonObject ob = jsonStore.getItem(id);
    assertThat(ob.getString("name"), is("thing11"));
  }

 @Test
  public void test5() {
    Map getByMap = new HashMap<String, String>();
    getByMap.put("name", "thing6");
    QuerySet qs = new QuerySet()
            .setLeft(new Query()
              .setField("name").setOperator(Operator.EQUALS).setValue("thing6"))
            .setOperator(BooleanOperator.AND)
            .setRight(Boolean.TRUE);
    MockCollection jsonList = jsonStore.getCollection(null, 1, qs);
    JsonObject ob = jsonList.getObjectList().get(0);
    assertNotNull(ob);
    assertThat(jsonList.getObjectList(), hasSize(1));
    assertTrue(ob.containsKey("name"));
    assertNotNull(ob.getString("name"));
    assertThat(ob.getString("name"), is("thing6"));
  }

  @Test
  public void testQueryset() {

    Query sizeQuery = new Query().setField("size").setOperator(Operator.EQUALS)
            .setValue("small");
    Query speciesQuery = new Query().setField("species")
            .setOperator(Operator.EQUALS).setValue("cat");
    Query colorQuery = new Query().setField("color")
            .setOperator(Operator.EQUALS).setValue("black");
    QuerySet smallBlackCatQS = new QuerySet().setLeft(sizeQuery)
            .setOperator(BooleanOperator.AND).setRight(
                    new QuerySet().setLeft(speciesQuery)
                    .setOperator(BooleanOperator.AND).setRight(colorQuery));
    System.out.println("Queryset dump: " + smallBlackCatQS.toString() + "\n");
    JsonArray resultList = filterList(thingList, smallBlackCatQS);

    assertThat(resultList.size(), is(1));
    assertThat(resultList.getJsonObject(0).getString("name"), is("hector"));

  }

  @Test
  public void testQuerySetCQL() throws CQLParseException {
    String query = "size == small and species == cat and color == black";
    QuerySet qs = QuerySet.fromCQL(query);
    JsonArray resultList = filterList(thingList, qs);
    System.out.println("Queryset dump: " + qs + "\n");
    assertNotNull(resultList);
    assertThat(resultList.size(), is(1));
    assertThat(resultList.getJsonObject(0).getString("name"), is("hector"));
  }

  private JsonArray filterList(JsonArray list, QuerySet qs) {
    JsonArray resultList = new JsonArray();
    for(Object ob : list) {
      if(qs.match((JsonObject) ob)) {
        resultList.add(ob);
      }
    }
    return resultList;
  }

}
