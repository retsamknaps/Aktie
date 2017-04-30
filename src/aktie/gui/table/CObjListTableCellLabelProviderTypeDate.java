package aktie.gui.table;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.lucene.search.SortField;

import aktie.data.CObj;

public class CObjListTableCellLabelProviderTypeDate extends CObjListTableCellLabelProvider
{

    private SimpleDateFormat dateformat;

    public CObjListTableCellLabelProviderTypeDate ( String key, boolean privateAttribute, String highlightKey )
    {
        super ( key, privateAttribute, SortField.Type.LONG, highlightKey );

        dateformat = new SimpleDateFormat ( "d MMM yyyy HH:mm z" );
    }

    @Override
    public String getFormattedAttribute ( CObj co )
    {
        if ( co != null )
        {
            Long attribute = null;

            if ( isPrivateAttribute() )
            {
                attribute = co.getPrivateNumber ( getKey() );
            }

            else
            {
                attribute = co.getNumber ( getKey() );
            }

            if ( attribute != null )
            {
                return dateformat.format ( new Date ( attribute.longValue() ) );
            }

        }

        return "";
    }

}
