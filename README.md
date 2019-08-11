This project implemented: 
* Rest API that can handle any structured data in Json
* Rest API with support for crud operations, including merge support, cascaded delete
* Rest API with support for validation
* Json Schema describing the data model for the use case
* Advanced semantics with rest API operations such as get if not changed
* Storage of data in key/value store


Have tried to apply Spring Boot Filter, but couldn't be able to find out why it filtered out all urls (not only /plans/*, but /auth). So had to verify token for each controller instead. 
In the 1st version, I used recursion to deal with nested objects. 
