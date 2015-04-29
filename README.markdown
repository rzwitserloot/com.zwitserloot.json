# com.zwitserloot.json

### Downloading

You can download source, javadoc, and binaries from [https://github.com/rzwitserloot/com.zwitserloot.json/releases](https://github.com/rzwitserloot/com.zwitserloot.json/releases)

## How to compile / develop

run:

	ant

and that's that. All dependencies will be downloaded automatically. Once you've run ant, you can also open the project directory as an eclipse project.
The runtime jar will be in the `dist` directory.

## General principle

JSON comes from javascript, which is not an explicitly typed language. Therefore, when trying to work with JSON from java, it is a good idea to be explicit about what type you
think some element in the JSON data should be and coerce whatever's there to this type; this matches how javascript and other similar languages deal with JSON.

This library does just that: You treat JSON as a directory-like structure and then query keys by stating what type you think it should be. This library will then find this element
in the JSON and goes to some lengths to coerce it to the requested type, including parsing strings into numbers and upgrading single elements into lists with 1 element in them, if
that's what you expected. Furthermore, all query methods have a second form with a default value to return if the element isn't in the JSON data.

This is _not_ a library for 'marshalling' (the notion of converting JSON into java POJOs and back). That is much more complicated and heavyweight.

## How to use

The simplest example of reading something:

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

The above snippet also shows how all the `asX` methods take an optional default.

It is perfectly acceptable to `get()` your way into non-existent nodes; this does not cause an error, and if you try to get a value from such a non-existent node, you'll always get the default, or an exception if you didn't specify a default, or an empty list / keyset if you try to coerce the value to a list or map. This is not only convenient, as many JSON services simply omit information that isn't available, but is also the mechanism with which you can create new JSON. For example, to recreate the above JSON programatically:

	JSON json = JSON.newMap();
	JSON serenity = json.get("films").add();
	serenity.get("name").setString("Serenity");
	serenity.get("director").get("name").setString("Joss Whedon");
	serenity.get("director").get("age").setInt(45);
	JSON fewGoodMen = json.get("films").add();
	fewGoodMen.get("name").setString("A few good men");
	fewGoodMen.get("director").get("name").setString("Rob Reiner");
	String jsonString = json.prettyPrint();

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

The `asList()` method is smart enough to have zero size if the node doesn't exist, and to form a list containing just one element if you're on a simple (non-list, non-map) element.

As a convenience, there's also `asStringList()` which will coerce all elements inside the list to a string.

### Writing lists and maps

You've already seen how to write maps; just `get` the key name then start using the `setX` methods. To write into a list, use the magic `add()` method. add() itself doesn't actually create anything, but once you start writing to a JSON instance returned by the `add()` method, writing occurs. See the 'write' example from earlier.

#### Printing

A JSON object can be rendered as minified JSON with the `toJSON()` method, or as pretty-printed JSON with the `prettyPrint()` method.

### Advanced topics

* You can use `mixin(JSON)` to merge 2 separate JSON lists or 2 separate JSON maps.
* You can use `setWithJSON(JSON)` to put some JSON inside another JSON object.
* You can use `setIsMap()` and `setIsList()` to enforce empty maps/lists.
* You can use `deepCopy()` to create a 'deep' clone such that changes to one will not affect the other.
* You can use `getPath()`, `up()` and `top()` to treat the JSON object as a directory pointer of sorts.

### Changelog

#### v1.2

* Pretty printing
