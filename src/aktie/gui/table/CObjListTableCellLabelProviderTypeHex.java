package aktie.gui.table;

import java.util.Formatter;

import org.apache.lucene.search.SortField;

import aktie.crypto.Utils;
import aktie.data.CObj;

public class CObjListTableCellLabelProviderTypeHex extends CObjListTableCellLabelProvider
{
    public CObjListTableCellLabelProviderTypeHex ( String key, boolean privateAttribute, String highlightKey )
    {
        super ( key, privateAttribute, SortField.Type.STRING, highlightKey );
    }

    @Override
    public String getFormattedAttribute ( CObj co )
    {
        if ( co != null )
        {
            String attribute = null;

            if ( isPrivateAttribute() )
            {
                attribute = co.getPrivate ( getKey() );
            }

            else
            {
                attribute = co.getString ( getKey() );
            }

            if ( attribute != null )
            {
                byte bytes[] = Utils.toByteArray ( attribute );
                StringBuilder sb = new StringBuilder();
                Formatter formatter = new Formatter ( sb );

                for ( int i = 0; i < bytes.length; i++ )
                {
                    formatter.format ( "%02X", 0xFF & bytes[i] );
                }

                formatter.close();

                return sb.toString();
            }

        }

        return "";
    }

}
