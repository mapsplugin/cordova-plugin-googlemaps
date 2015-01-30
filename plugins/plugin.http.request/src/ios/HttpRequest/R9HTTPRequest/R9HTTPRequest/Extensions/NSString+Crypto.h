//
// NSString+Crypto.h
//
// About
// -----
//
// CocoaCryptoHashing is a very simple and lightweight collection of MD5/SHA1
// functions for Cocoa (which sadly does not contain any of these by default). It
// provides two categories on both `NSString` and `NSData`. The header file is
// pretty well documented, so getting started should not be a problem.
//
// Requirements
// ------------
//
// You will need Cocoa (and more specifically, Foundation), in order to use
// this. OpenSSL is required as well, but since Mac OS X comes with it by
// default, this should not be a problem.
//
// Building
// --------
//
// Depending on the platform and the target, you may need different linker flags.
// On 10.5 and on the iPhone platform, `-lcrypto` should be enough; on 10.4 you
// may need to use both `-lcrypto` and `-lssl`.
//
// License
// -------
//
// CocoaCryptoHashing is licensed under the modified BSD license. This license is
// included in the COPYING file.
//
// Contact
// -------
//
// Any comments, questions, remarks, ... should be sent to
// <denis.defreyne@stoneship.org>.
//

#import <Foundation/Foundation.h>

@interface NSString (Crypto)

/*!
 * @method md5Hash
 * @abstract Calculates the MD5 hash from the UTF-8 representation of the specified string  and returns the binary representation
 * @result A NSData object containing the binary representation of the MD5 hash
 */
- (NSData *)md5Hash;

/*!
 * @method md5HexHash
 * @abstract Calculates the MD5 hash from the UTF-8 representation of the specified string and returns the hexadecimal representation
 * @result A NSString object containing the hexadecimal representation of the MD5 hash
 */
- (NSString *)md5HexHash;

/*!
 * @method sha1Hash
 * @abstract Calculates the SHA-1 hash from the UTF-8 representation of the specified string  and returns the binary representation
 * @result A NSData object containing the binary representation of the SHA-1 hash
 */
- (NSData *)sha1Hash;

/*!
 * @method sha1HexHash
 * @abstract Calculates the SHA-1 hash from the UTF-8 representation of the specified string and returns the hexadecimal representation
 * @result A NSString object containing the hexadecimal representation of the SHA-1 hash
 */
- (NSString *)sha1HexHash;

@end
