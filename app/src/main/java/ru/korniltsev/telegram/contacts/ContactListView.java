package ru.korniltsev.telegram.contacts;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import com.tonicartos.superslim.LayoutManager;
import mortar.dagger1support.ObjectGraphService;
import ru.korniltsev.telegram.chat.R;
import ru.korniltsev.telegram.common.recycler.sections.Section;
import ru.korniltsev.telegram.core.flow.pathview.TraversalAware;
import ru.korniltsev.telegram.core.toolbar.ToolbarUtils;

import javax.inject.Inject;
import java.util.List;

public class ContactListView extends LinearLayout implements TraversalAware{
    @Inject ContactsPresenter presenter;
    private RecyclerView list;
    private ContactsAdapter adapter;
    private boolean traversalCompleted;
    private Runnable afterTraversal;

    public ContactListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        ObjectGraphService.inject(context, this);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        ToolbarUtils.initToolbar(this)
                .setTitle(R.string.contacts)
                .pop();
        list = ((RecyclerView) findViewById(R.id.list));
        list.setLayoutManager(new LayoutManager(getContext()));
        adapter = new ContactsAdapter(getContext(), presenter);
        list.setAdapter(adapter);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        presenter.takeView(this);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        presenter.dropView(this);
    }

    public void showContacts(final List<Contact> response) {
        executeAftertraversal(new Runnable() {
            @Override
            public void run() {
                showContactsImpl(response);
            }
        });

    }

    private void showContactsImpl(List<Contact> response) {
        adapter.addAll(
                Section.prepareListOf(
                        response, new Section.SectionFactory<Contact>() {
                            @Override
                            public String sectionForItem(Contact user) {
                                return user.section;
                            }

                            @Override
                            public long id(Contact user) {
                                return user.user.id;
                            }
                        }));
    }

    public void executeAftertraversal(Runnable run){
        if (traversalCompleted){
            run.run();
        } else {
            afterTraversal = run;
        }
    }

    @Override
    public void onTraversalCompleted() {
        traversalCompleted = true;
        if (afterTraversal != null) {
            afterTraversal.run();
            afterTraversal = null;
        }
    }
}
