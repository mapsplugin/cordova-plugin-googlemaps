#define MAX_ELEMENTS 100
#define MAX_ATTRIBUTES 100

#define TBXML_ATTRIBUTE_NAME_START  0
#define TBXML_ATTRIBUTE_NAME_END    1
#define TBXML_ATTRIBUTE_VALUE_START 2
#define TBXML_ATTRIBUTE_VALUE_END   3
#define TBXML_ATTRIBUTE_CDATA_END   4

#undef _FILE_OFFSET_BITS

/** The TBXMLAttribute structure holds information about a single XML attribute. The structure holds the attribute name, value and next sibling attribute. This structure allows us to create a linked list of attributes belonging to a specific element.
 */
typedef struct _TBXMLAttribute {
	char * name;
	char * value;
	struct _TBXMLAttribute * next;
} TBXMLAttribute;

/** The TBXMLElement structure holds information about a single XML element. The structure holds the element name & text along with pointers to the first attribute, parent element, first child element and first sibling element. Using this structure, we can create a linked list of TBXMLElements to map out an entire XML file.
 */
typedef struct _TBXMLElement {
	char * name;
	char * text;

	TBXMLAttribute * firstAttribute;

	struct _TBXMLElement * parentElement;

	struct _TBXMLElement * firstChild;
	struct _TBXMLElement * currentChild;

	struct _TBXMLElement * nextSibling;
	struct _TBXMLElement * previousSibling;

} TBXMLElement;


/** The TBXMLElementBuffer is a structure that holds a buffer of TBXMLElements.
 *  When the buffer of elements is used, an additional buffer is created and linked
 *  to the previous one. This allows for efficient memory allocation/deallocation
 *  elements.
 */
typedef struct _TBXMLElementBuffer {
	TBXMLElement               *elements;
	struct _TBXMLElementBuffer *next;
	struct _TBXMLElementBuffer *previous;
} TBXMLElementBuffer;


/** The TBXMLAttributeBuffer is a structure that holds a buffer of TBXMLAttributes.
 *  When the buffer of attributes is used, an additional buffer is created and linked
 *  to the previous one. This allows for efficient memeory allocation/deallocation of
 *  attributes.
 */
typedef struct _TBXMLAttributeBuffer {
	TBXMLAttribute               *attributes;
	struct _TBXMLAttributeBuffer *next;
	struct _TBXMLAttributeBuffer *previous;
} TBXMLAttributeBuffer;


// TBXMLDocument
// Container for the allocated memory that persists across JNI calls.
typedef struct _TBXMLDocument {
	jbyte                *xml;
	jsize                 length;
	TBXMLElement         *rootXMLElement;
	long                  currentElement;
	TBXMLElementBuffer   *currentElementBuffer;
	long                  currentAttribute;
	TBXMLAttributeBuffer *currentAttributeBuffer;
} TBXMLDocument;
