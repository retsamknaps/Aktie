package aktie.gui;

import aktie.data.CObj;

import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

public class IdentitySubTreeProvider implements ITreeContentProvider
{

    public IdentitySubTreeProvider()
    {
    }

    public class TreeSubscription
    {
        public TreeSubscription ( TreeIdentity p, CObj c )
        {
            parent = p;
            community = c;
        }

        public TreeIdentity parent;
        public CObj community;
        public boolean equals ( Object o )
        {
            if ( ! ( o instanceof TreeSubscription ) )
            {
                return false;
            }

            TreeSubscription ts = ( TreeSubscription ) o;
            return ts.community.getDig().equals ( community.getDig() );
        }

        public int hashCode()
        {
            return community.getDig().hashCode();
        }

    }

    public class TreeIdentity
    {
        public TreeIdentity ( CObj i, Map<String, CObj> m )
        {
            identity = i;
            children = m;
        }

        public CObj identity;
        public Map<String, CObj> children;
        public boolean equals ( Object o )
        {
            if ( ! ( o instanceof TreeIdentity ) ) { return false; }

            TreeIdentity t = ( TreeIdentity ) o;
            return identity.getId().equals ( t.identity.getId() );
        }

        public int hashCode()
        {
            return identity.getId().hashCode();
        }

    }

    @Override
    public void dispose()
    {
    }

    @Override
    public void inputChanged ( Viewer arg0, Object arg1, Object arg2 )
    {
    }

    @Override
    public Object[] getChildren ( Object a )
    {
        if ( a instanceof TreeIdentity )
        {
            TreeIdentity e = ( TreeIdentity ) a;
            Map<String, CObj> m = e.children;
            Collection<CObj> c = m.values();
            Object r[] = new Object[c.size()];
            int idx = 0;

            for ( CObj i : c )
            {
                r[idx] = new TreeSubscription ( e, i );
                idx++;
            }

            return r;
        }

        return new Object[] {};

    }

    @Override
    public Object[] getElements ( Object a )
    {
        if ( a instanceof IdentitySubTreeModel )
        {
            IdentitySubTreeModel m = ( IdentitySubTreeModel ) a;
            Object r[] = new Object[m.getSubCommunities().size()];
            int idx = 0;

            for ( Entry<String, SortedMap<String, CObj>> e : m.getSubCommunities().entrySet() )
            {
                CObj id = m.getIdentities().get ( e.getKey() );
                r[idx] = new TreeIdentity ( id, e.getValue() );
                idx++;
            }

            return r;
        }

        return new Object[] {};

    }

    @Override
    public Object getParent ( Object a )
    {
        if ( a instanceof TreeSubscription )
        {
            TreeSubscription ts = ( TreeSubscription ) a;
            return ts.parent;
        }

        return null;
    }

    @Override
    public boolean hasChildren ( Object a )
    {
        if ( a instanceof TreeIdentity )
        {
            TreeIdentity m = ( TreeIdentity ) a;
            return ( m.children.size() > 0 );
        }

        return false;
    }

}
