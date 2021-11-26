Castor XML Unmarshalling CWE 502 examples
=====

This project has an example of using Castor to try to deserialize 
to arbitrary classes (CWE 502 flaw).

While this appears to be possible with version 1.3.1
it does not appear to be possible with version 0.9.5.

Castor 0.9.5 documentation does say:
> By default, if Castor finds no information for a given class in the mapping file, it will introspect the class and apply a set of default rules to guess the fields and marshal them. The default rules are as follows:
> 
> -	All primitive types, including the primitive type wrappers (Boolean, Short, etc...) are marshalled as attributes.
> -	All other objects are marshalled as elements with either text content or element content.

But it does not appear to work?
