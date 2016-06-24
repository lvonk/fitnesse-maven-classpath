# Introduction.

FitNesse plugin that provides Maven Classpath support.

# How to use.

 - Download the distribution.
 - Get yourself an up-to-date copy of fitnesse (>= 20150114)
 - Add the following line to plugins.properties:
 
       SymbolTypes = fitnesse.wikitext.widgets.MavenClasspathSymbolType

 - Refer to the pom file as follows:
 
       !pomFile /path/to/pom.xml
        
 - you can define the file as `pom.xml@compile` to include a specific scope.

# How to contribute.

 - Fork the repository and send pull requests.


