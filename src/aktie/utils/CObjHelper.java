package aktie.utils;

import java.io.IOException;

import aktie.crypto.Utils;
import aktie.data.CObj;
import aktie.index.CObjList;

public class CObjHelper
{

    private CObjHelper() {}

    // HasPart
    /**
        Create a raw byte array in which the bits can be used to store the fragment indices
        of the already obtained fragments of a part file.
        @param totalFragments The number of total fragments which is the minimum bit capacity of the byte array.
        @return An array with a minimum bit capacity of totalFragments with all bits set to 0.
    */
    private static byte[] createRawHasPartArray ( long totalFragments )
    {
        return Utils.createAllZeroByteArray ( ( int ) totalFragments );
    }

    // HasPart
    /**
        Create a base64 encoded string of the byte array using UTF-8 based encoding.
        The base64 encoded string can be re-created to a byte array by decodeBase64HasPartArray().
        Using the string, fragment indices of the already obtained fragments of a part file
        can be pushed to a CObj as payload string.
        @param hasPartArray The byte array from which to create the base64 encoded string.
        @return The base64 representation of the byte array.
    */
    private static String encodeBase64HasPartArray ( byte[] hasPartArray )
    {
        return Utils.toString ( hasPartArray );
    }

    // HasPart
    /**
        (Re-)create a byte array from a base64 encoded string using UTF-8 based decoding.
        The base64 encoded string should have been created from a byte array by encodeBase64HasPartArray().
        @param payload The base64 string from which to create the byte array.
        @return The byte array retrieved from the base64 string.
    */
    private static byte[] decodeBase64HasPartPayload ( String payload )
    {
        return Utils.toByteArray ( payload );
    }

    // HasPart
    /**
        Create the payload string that can be pushed to a CObj to represent the completed fragments.
        The prerequiste is that the fragments in the CObjList have the number CObj.FRAGINDEX set.
        @param completedFragments A CObjList which references the completed fragments of a part file.
        @param fileSize The size of the file the fragment belongs to.
        @param totalFragments The total number of fragments of this file.
        @return A base64 encoded string (UTF-8 based encoding) representing a byte array where bits set to 1
               represent the indexes of the completed fragments.
    */
    public static String createHasPartPayload ( CObjList completedFragments, long fileSize, long totalFragments )
    {
        byte[] fragIndexArray = CObjHelper.createRawHasPartArray ( totalFragments );

        for ( int i = 0; i < completedFragments.size(); i++ )
        {
            try
            {
                CObj completedFragment = completedFragments.get ( i );

                Long fragOffset = completedFragment.getNumber ( CObj.FRAGOFFSET );
                Long fragSize = completedFragment.getNumber ( CObj.FRAGSIZE );

                if ( fragOffset != null && fragSize != null )
                {
                    int fragIndex = calculateFragmentIndex ( fileSize, totalFragments, fragOffset.longValue(), fragSize.longValue() );

                    if ( fragIndex < totalFragments && fragIndex < fragIndexArray.length )
                    {
                        Utils.setBit ( fragIndexArray, fragIndex, true );
                    }

                }



            }

            catch ( IOException e )
            {

            }


        }

        return encodeBase64HasPartArray ( fragIndexArray );
    }

    // HasPart
    /**
        Check whether the part file information object's payload says that a complete fragment is available
        at the specified fragment offset.
        @param payload A base64 encoded string (UTF-8 based encoding) representing a byte array where bits set to 1
                      represent the offsets of the completed fragments.
        @param fragIndex The fragment index.
        @return true if the byte array says that the fragment is available, false otherwise
               or if an ArrayIndexOutOfBoundsException is thrown during the lookup.
    */
    public static final boolean hasPartPayloadListsFragment ( String payload, int fragIndex )
    {
        byte[] fragIndexArray = decodeBase64HasPartPayload ( payload );

        try
        {
            return Utils.isBitSet ( fragIndexArray, fragIndex );
        }

        catch ( ArrayIndexOutOfBoundsException e )
        {
            return false;
        }

    }

    // HasPart
    /**
        Calculate the index of a file fragment based on its offset and file size, and, in case of
        the last fragment, based on its offset, the fragment size and the file size.
        Assumption: All fragments have the same size except the last fragment which might be smaller.
        @param fileSize The size of the file the fragment belongs to.
        @param totalFragments The total number of fragments of this file.
        @param fragOffset The byte offset where the start of this fragment is located in the file.
        @param fragSize The actual size of the fragment (last fragment of a file might be smaller)
                       or the nominal fragment size, even in case of the last fragment if its actual size is not known.
        @return The index of the fragment in the file.
    */
    public static final int calculateFragmentIndex ( long fileSize, long totalFragments, long fragOffset, long fragSize )
    {
        // Last fragment which might have a different size than the other fragments
        // As we might not know the actual size of the last fragment, test condition is greater or equal.
        if ( fragOffset + fragSize >= fileSize )
        {
            return ( int ) totalFragments - 1;
        }

        return ( int ) ( fragOffset / fragSize );
    }

}
