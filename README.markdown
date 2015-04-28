# com.zwitserloot.json

## How to compile / develop

run:

	ant

and that's that. All dependencies will be downloaded automatically. Once you've run ant, you can also open the project directory as an eclipse project.
The runtime jar will be in the `dist` directory.

## How to use

The general idea is that you work with an instance of the `JSON` class, and specifying the type you expected / want directly. So, to read something:

	String value = json.asString();
	int value = json.asInt();

Similarly, to set a new value:

	json.setString("Hello, world!");
	json.setInt(10);

A JSON instance isn't just a representation of an actual JSON structure, but it also includes your position inside it; a JSON blob has structure, after all. For example, if we have:

  {
    films: [
      {
        name: "Serenity",
        director: {
          name: "Joss Whedon",
          age: 45
        }
      },
      {
        name: "A few good men",
        director: {
          name: "Rob Reiner"
        }
      }
    ]
  }

then you can query values from it like so:

	JSON json = JSON.parse(theAboveJSONString);
	int jossWhedonsAge = json.get("films").get(0).get("director").get("age").asInt(-1);

The above snippet also shows how all the asX methods take an optional default.

It is perfectly acceptable to `get` your way into non-existent nodes; this does not cause an error, and if you try to get a value from such a non-existent node, you'll always get the default, or an exception if you didn't specify a default. This is not only convenient, as many JSON services simply omit information that isn't available, but is also the mechanism with which you can create new JSON. For example, to recreate the above JSON programatically:

	JSON json = JSON.newMap();
	JSON serenity = json.get("films").add();
	serenity.get("name").setString("Serenity");
	serenity.get("director").get("name").setString("Joss Whedon");
	serenity.get("director").get("age").setInt(45);
	JSON fewGoodMen = json.get("films").add();
	fewGoodMen.get("name").setString("A few good men");
	fewGoodMen.get("director").get("name").setString("Rob Reiner");
	String jsonString = json.toJSON();

### Reading from lists and maps

Once you've navigated your way to a map node, you can use `keySet()` to loop through each key. In JSON, all keys are always strings. For example:

	JSON json = JSON.parse(movieData);
	for (String key : json.get("films").get(0).keySet()) {
		System.out.println(key);
	}

would print `name` followed by `director`.

To navigate through a list node, use the `asList()` method:

	for (JSON movie : JSON.parse(movieData).get("films").asList()) {
		...
	}

The `asList()` method is smart enough to have zero size if the node doesn't exist, and to form a list containing just one element if you're on a simple (non-list, non-map) element. In fact, this applies generally to the com.zwitserloot.json library: For example, trying to grab a string node via 'asInt()' will attempt to parse the string as an int.

There are also a few convenience methods, such as a way to treat a json object as a list of strings: `asStringList()`.

### Writing lists and maps

You've already seen how to write maps; just `get` the key name then start using the `setX` methods. To write into a list, use the magic `add()` method. add() itself doesn't actually create anything, but once you start writing to a JSON instance returned by the `add()` method, writing occurs. See the 'write' example from earlier.

### Downloading

You can download source, javadoc, and binaries from [http://github.com/rzwitserloot/com.zwitserloot.json/downloads](http://github.com/rzwitserloot/com.zwitserloot.json/downloads)
