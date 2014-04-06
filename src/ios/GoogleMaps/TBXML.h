// ================================================================================================
//  TBXML.h
//  Fast processing of XML files
//
// ================================================================================================
//  Created by Tom Bradley on 21/10/2009.
//  Version 1.5
//  
//  Copyright 2012 71Squared All rights reserved.b
//  
//  Permission is hereby granted, free of charge, to any person obtaining a copy
//  of this software and associated documentation files (the "Software"), to deal
//  in the Software without restriction, including without limitation the rights
//  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
//  copies of the Software, and to permit persons to whom the Software is
//  furnished to do so, subject to the following conditions:
//  
//  The above copyright notice and this permission notice shall be included in
//  all copies or substantial portions of the Software.
//  
//  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
//  THE SOFTWARE.
// ================================================================================================

@class TBXML;


// ================================================================================================
//  Error Codes
// ================================================================================================
enum TBXMLErrorCodes {
    D_TBXML_SUCCESS = 0,

    D_TBXML_DATA_NIL,
    D_TBXML_DECODE_FAILURE,
    D_TBXML_MEMORY_ALLOC_FAILURE,
    D_TBXML_FILE_NOT_FOUND_IN_BUNDLE,
    
    D_TBXML_ELEMENT_IS_NIL,
    D_TBXML_ELEMENT_NAME_IS_NIL,
    D_TBXML_ELEMENT_NOT_FOUND,
    D_TBXML_ELEMENT_TEXT_IS_NIL,
    D_TBXML_ATTRIBUTE_IS_NIL,
    D_TBXML_ATTRIBUTE_NAME_IS_NIL,
    D_TBXML_ATTRIBUTE_NOT_FOUND,
    D_TBXML_PARAM_NAME_IS_NIL
};


// ================================================================================================
//  Defines
// ================================================================================================
#define D_TBXML_DOMAIN @"com.71squared.tbxml"

#define MAX_ELEMENTS 100
#define MAX_ATTRIBUTES 100

#define TBXML_ATTRIBUTE_NAME_START 0
#define TBXML_ATTRIBUTE_NAME_END 1
#define TBXML_ATTRIBUTE_VALUE_START 2
#define TBXML_ATTRIBUTE_VALUE_END 3
#define TBXML_ATTRIBUTE_CDATA_END 4

// ================================================================================================
//  Structures
// ================================================================================================

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

/** The TBXMLElementBuffer is a structure that holds a buffer of TBXMLElements. When the buffer of elements is used, an additional buffer is created and linked to the previous one. This allows for efficient memory allocation/deallocation elements.
 */
typedef struct _TBXMLElementBuffer {
	TBXMLElement * elements;
	struct _TBXMLElementBuffer * next;
	struct _TBXMLElementBuffer * previous;
} TBXMLElementBuffer;



/** The TBXMLAttributeBuffer is a structure that holds a buffer of TBXMLAttributes. When the buffer of attributes is used, an additional buffer is created and linked to the previous one. This allows for efficient memeory allocation/deallocation of attributes.
 */
typedef struct _TBXMLAttributeBuffer {
	TBXMLAttribute * attributes;
	struct _TBXMLAttributeBuffer * next;
	struct _TBXMLAttributeBuffer * previous;
} TBXMLAttributeBuffer;


// ================================================================================================
//  Block Callbacks
// ================================================================================================
typedef void (^TBXMLSuccessBlock)(TBXML *tbxml);
typedef void (^TBXMLFailureBlock)(TBXML *tbxml, NSError *error);
typedef void (^TBXMLIterateBlock)(TBXMLElement *element);
typedef void (^TBXMLIterateAttributeBlock)(TBXMLAttribute *attribute, NSString *attributeName, NSString *attributeValue);


// ================================================================================================
//  TBXML Public Interface
// ================================================================================================

@interface TBXML : NSObject {
	
@private
	TBXMLElement * rootXMLElement;
	
	TBXMLElementBuffer * currentElementBuffer;
	TBXMLAttributeBuffer * currentAttributeBuffer;
	
	long currentElement;
	long currentAttribute;
	
	char * bytes;
	long bytesLength;
}


@property (nonatomic, readonly) TBXMLElement * rootXMLElement;

+ (id)newTBXMLWithXMLString:(NSString*)aXMLString error:(NSError **)error;
+ (id)newTBXMLWithXMLData:(NSData*)aData error:(NSError **)error;
+ (id)newTBXMLWithXMLFile:(NSString*)aXMLFile error:(NSError **)error;
+ (id)newTBXMLWithXMLFile:(NSString*)aXMLFile fileExtension:(NSString*)aFileExtension error:(NSError **)error;

+ (id)newTBXMLWithXMLString:(NSString*)aXMLString __attribute__((deprecated));
+ (id)newTBXMLWithXMLData:(NSData*)aData __attribute__((deprecated));
+ (id)newTBXMLWithXMLFile:(NSString*)aXMLFile __attribute__((deprecated));
+ (id)newTBXMLWithXMLFile:(NSString*)aXMLFile fileExtension:(NSString*)aFileExtension __attribute__((deprecated));


- (id)initWithXMLString:(NSString*)aXMLString error:(NSError **)error;
- (id)initWithXMLData:(NSData*)aData error:(NSError **)error;
- (id)initWithXMLFile:(NSString*)aXMLFile error:(NSError **)error;
- (id)initWithXMLFile:(NSString*)aXMLFile fileExtension:(NSString*)aFileExtension error:(NSError **)error;

- (id)initWithXMLString:(NSString*)aXMLString __attribute__((deprecated));
- (id)initWithXMLData:(NSData*)aData __attribute__((deprecated));
- (id)initWithXMLFile:(NSString*)aXMLFile __attribute__((deprecated));
- (id)initWithXMLFile:(NSString*)aXMLFile fileExtension:(NSString*)aFileExtension __attribute__((deprecated));


- (int) decodeData:(NSData*)data;
- (int) decodeData:(NSData*)data withError:(NSError **)error;

@end

// ================================================================================================
//  TBXML Static Functions Interface
// ================================================================================================

@interface TBXML (StaticFunctions)

+ (NSString*) elementName:(TBXMLElement*)aXMLElement;
+ (NSString*) elementName:(TBXMLElement*)aXMLElement error:(NSError **)error;
+ (NSString*) textForElement:(TBXMLElement*)aXMLElement;
+ (NSString*) textForElement:(TBXMLElement*)aXMLElement error:(NSError **)error;
+ (NSString*) valueOfAttributeNamed:(NSString *)aName forElement:(TBXMLElement*)aXMLElement;
+ (NSString*) valueOfAttributeNamed:(NSString *)aName forElement:(TBXMLElement*)aXMLElement error:(NSError **)error;

+ (NSString*) attributeName:(TBXMLAttribute*)aXMLAttribute;
+ (NSString*) attributeName:(TBXMLAttribute*)aXMLAttribute error:(NSError **)error;
+ (NSString*) attributeValue:(TBXMLAttribute*)aXMLAttribute;
+ (NSString*) attributeValue:(TBXMLAttribute*)aXMLAttribute error:(NSError **)error;

+ (TBXMLElement*) nextSiblingNamed:(NSString*)aName searchFromElement:(TBXMLElement*)aXMLElement;
+ (TBXMLElement*) childElementNamed:(NSString*)aName parentElement:(TBXMLElement*)aParentXMLElement;

+ (TBXMLElement*) nextSiblingNamed:(NSString*)aName searchFromElement:(TBXMLElement*)aXMLElement error:(NSError **)error;
+ (TBXMLElement*) childElementNamed:(NSString*)aName parentElement:(TBXMLElement*)aParentXMLElement error:(NSError **)error;

/** Iterate through all elements found using query.
 
 Inspiration taken from John Blanco's RaptureXML https://github.com/ZaBlanc/RaptureXML
 */
+ (void)iterateElementsForQuery:(NSString *)query fromElement:(TBXMLElement *)anElement withBlock:(TBXMLIterateBlock)iterateBlock;
+ (void)iterateAttributesOfElement:(TBXMLElement *)anElement withBlock:(TBXMLIterateAttributeBlock)iterateBlock;


@end
