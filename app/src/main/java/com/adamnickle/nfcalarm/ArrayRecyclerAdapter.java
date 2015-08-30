package com.adamnickle.nfcalarm;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

/**
 * Created by Adam on 4/17/2015.
 */
public abstract class ArrayRecyclerAdapter<T, VH extends RecyclerView.ViewHolder> extends RecyclerView.Adapter<VH> implements List<T>
{
    protected final ArrayList<T> mElements;

    public ArrayRecyclerAdapter()
    {
        mElements = new ArrayList<>();
    }

    public ArrayRecyclerAdapter( Collection<T> elements )
    {
        this();
        if( elements != null )
        {
            addAll( elements );
        }
    }

    @Override
    public int getItemCount()
    {
        return size();
    }

    @Override
    public void add( int location, T object )
    {
        mElements.add( location, object );
        notifyItemInserted( location );
    }

    @Override
    public boolean add( T object )
    {
        final boolean added = mElements.add( object );
        if( added )
        {
            notifyItemInserted( mElements.size() );
        }
        return added;
    }

    @Override
    public boolean addAll( int location, Collection<? extends T> collection )
    {
        final boolean added = mElements.addAll( location, collection );
        if( added )
        {
            notifyItemRangeInserted( location, collection.size() );
        }
        return added;
    }

    @Override
    public boolean addAll( Collection<? extends T> collection )
    {
        final int size = mElements.size();
        final boolean added = mElements.addAll( collection );
        if( added )
        {
            notifyItemRangeInserted( size, collection.size() );
        }
        return added;
    }

    @Override
    public void clear()
    {
        final int size = mElements.size();
        mElements.clear();
        notifyItemRangeRemoved( 0, size );
    }

    @Override
    public boolean contains( Object object )
    {
        return mElements.contains( object );
    }

    @Override
    public boolean containsAll( Collection<?> collection )
    {
        return mElements.containsAll( collection );
    }

    @Override
    public T get( int location )
    {
        return mElements.get( location );
    }

    @Override
    public int indexOf( Object object )
    {
        return mElements.indexOf( object );
    }

    @Override
    public boolean isEmpty()
    {
        return mElements.isEmpty();
    }

    @NonNull
    @Override
    public Iterator<T> iterator()
    {
        final Iterator<T> iterator = mElements.iterator();
        return new Iterator<T>()
        {
            @Override
            public boolean hasNext()
            {
                return iterator.hasNext();
            }

            @Override
            public T next()
            {
                return iterator.next();
            }

            @Override
            @Deprecated
            public void remove()
            {
                throw new UnsupportedOperationException( "ArrayRecyclerAdapter.Iterator does not support remove()." );
            }
        };
    }

    @Override
    public int lastIndexOf( Object object )
    {
        return mElements.lastIndexOf( object );
    }

    @NonNull
    @Override
    public ListIterator<T> listIterator()
    {
        return listIterator( 0 );
    }

    @NonNull
    @Override
    public ListIterator<T> listIterator( int location )
    {
        final ListIterator<T> listIterator = mElements.listIterator( location );
        return new ListIterator<T>()
        {
            @Override
            @Deprecated
            public void add( T object )
            {
                throw new UnsupportedOperationException( "ArrayRecyclerAdapter.ListIterator does not support add()." );
            }

            @Override
            public boolean hasNext()
            {
                return listIterator.hasNext();
            }

            @Override
            public boolean hasPrevious()
            {
                return listIterator.hasPrevious();
            }

            @Override
            public T next()
            {
                return listIterator.next();
            }

            @Override
            public int nextIndex()
            {
                return listIterator.nextIndex();
            }

            @Override
            public T previous()
            {
                return listIterator.previous();
            }

            @Override
            public int previousIndex()
            {
                return listIterator.previousIndex();
            }

            @Override
            @Deprecated
            public void remove()
            {
                throw new UnsupportedOperationException( "ArrayRecyclerAdapter.ListIterator does not support remove()." );
            }

            @Override
            @Deprecated
            public void set( T object )
            {
                throw new UnsupportedOperationException( "ArrayRecyclerAdapter.ListIterator does not support set()." );
            }
        };
    }

    @Override
    public T remove( int location )
    {
        T object = mElements.remove( location );
        notifyItemRemoved( location );
        return object;
    }

    @Override
    public boolean remove( Object object )
    {
        int index = mElements.indexOf( object );
        boolean removed = mElements.remove( object );
        if( removed )
        {
            notifyItemRemoved( index );
        }
        return removed;
    }

    @Override
    public boolean removeAll( Collection<?> collection )
    {
        boolean modified = false;
        for( Object obj : collection )
        {
            if( remove( obj ) )
            {
                modified = true;
            }
        }
        return modified;
    }

    @Override
    public boolean retainAll( Collection<?> collection )
    {
        boolean modified = false;
        int i = 0;
        while( i < mElements.size() )
        {
            if( !collection.contains( mElements.get( i ) ) )
            {
                mElements.remove( i );
                modified = true;
            }
            else
            {
                i++;
            }
        }
        return modified;
    }

    @Override
    public T set( int location, T object )
    {
        T setted = mElements.set( location, object );
        notifyItemChanged( location );
        return setted;
    }

    @Override
    public int size()
    {
        return mElements.size();
    }

    @NonNull
    @Override
    @Deprecated
    public List<T> subList( int start, int end )
    {
        throw new UnsupportedOperationException( "ArrayRecyclerAdapter does not support subList()." );
    }

    @NonNull
    @Override
    @Deprecated
    public Object[] toArray()
    {
        throw new UnsupportedOperationException( "ArrayRecyclerAdapter does not support toArray()." );
    }

    @NonNull
    @Override
    @Deprecated
    public <T1> T1[] toArray( T1[] array )
    {
        throw new UnsupportedOperationException( "ArrayRecyclerAdapter does not support toArray()." );
    }
}
