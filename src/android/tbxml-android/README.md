tbxml-android: version 1.00.0

# TBXML - Android NDK port

*tbxml-android* is a port of the [71Squared][71Squared] TBXML XML parsing library (for iOS) to 
the Android NDK.

It is intended for those occasional cases where you want the speed of a SAX parser with the convenience
of a DOM and can live with a very bare bones XML parser implementation.

History
-------
The Android NDK port is the result of a small project unexpectedly running smack bang into the shockingly bad 
performance of the standard XPath implementation shipped with the Android runtime. The iOS version of the app 
was using TBXML to parse the XML document and since the TBXML project had a Java port of the implementation it 
was straightforward to convert from XPath to TBXML. A quick look at the TBXML Objective-C implementation suggested 
it would be straightforward to port it to the NDK and a couple of hours later a basic working implementation showed 
a useful performance improvement.

Whimsy led to benchmarking it against the standard DOM and SAX parsers (and later the alternative VTD-XML parsers),
and it seems TBXML still lives up to its 'super-fast and lightweight' claim.

Benchmarks
----------
A quick and dirty performance test on a stock Nexus 4 gives the following times (in milliseconds) to parse a small (40K) XML document:

    ---------------------------------
                  Min     Ave     Max  
    ---------------------------------
    XPath        3061    3143    3348   
    DOM            16      25      43  
    SAX             9      10      12  
    VTD            19      29      44  
    VTD-XPath     101     108     118  
    TBXML-Java     79     109     143  
    TBXML-NDK       4       5       9  
    ---------------------------------

References
----------

1. [Original TBXML project][tbxml]
2. [TBXML github project][github]
3. [VTD-XML][vtd]
4. [XPath.evaluate performance slows down (absurdly) over multiple calls][stackoverflow1]
5. [Fastest XML parser for small, simple documents in Java][stackoverflow2]

[71Squared]:      http://www.71squared.com
[tbxml]:          http://www.tbxml.co.uk/TBXML/TBXML_Free.html
[github]:         https://github.com/71squared/TBXML
[vtd]:            http://vtd-xml.sourceforge.net
[stackoverflow1]: http://stackoverflow.com/questions/3782618/xpath-evaluate-performance-slows-down-absurdly-over-multiple-calls
[stackoverflow2]: http://stackoverflow.com/questions/530064/fastest-xml-parser-for-small-simple-documents-in-java

