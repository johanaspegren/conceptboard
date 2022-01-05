package com.aspegrenide.conceptboard;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.aspegrenide.conceptboard.data.Aspect;
import com.aspegrenide.conceptboard.data.Challenge;
import com.aspegrenide.conceptboard.data.Concept;
import com.aspegrenide.conceptboard.data.Idea;
import com.aspegrenide.conceptboard.data.Problem;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    // Use FireBase database to handle ideas, problems and needs
    private DatabaseReference fbDatabaseReference;
    private DatabaseReference challengeCloudEndPoint;
    private DatabaseReference problemCloudEndPoint;
    private DatabaseReference ideaCloudEndPoint;
    private DatabaseReference conceptCloudEndPoint;

    ArrayList<Challenge> challenges;
    ArrayList<String> challengesTitles;
    ArrayList<Problem> problems;
    ArrayList<String> problemsTitles;
    ArrayList<Idea> ideas;
    ArrayList<String> ideasTitles;
    ArrayList<Concept> concepts;
    ArrayList<String> conceptsTitles;
    String LOG_TAG = "log_tag";

    RecyclerView recViewChallenges;
    RecyclerView recViewProblems;
    RecyclerView recViewIdeas;
    RecyclerView recViewConcepts;

    RecyclerView.LayoutManager recViewLayoutManagerChallenges;
    RecyclerView.Adapter recViewAdapterChallenges;

    RecyclerView.LayoutManager recViewLayoutManagerProblems;
    RecyclerView.Adapter recViewAdapterProblems;

    RecyclerView.LayoutManager recViewLayoutManagerIdeas;
    RecyclerView.Adapter recViewAdapterIdeas;

    RecyclerView.LayoutManager recViewLayoutManagerConcepts;
    RecyclerView.Adapter recViewAdapterConcepts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        fbDatabaseReference =  FirebaseDatabase.getInstance("https://minide-1d8d6-default-rtdb.europe-west1.firebasedatabase.app").getReference();
        challengeCloudEndPoint = fbDatabaseReference.child("challenges");
        challengeCloudEndPoint.addValueEventListener(genericListener);

        problemCloudEndPoint = fbDatabaseReference.child("problems");
        problemCloudEndPoint.addValueEventListener(genericListener);

        ideaCloudEndPoint = fbDatabaseReference.child("ideas");
        ideaCloudEndPoint.addValueEventListener(genericListener);

        conceptCloudEndPoint = fbDatabaseReference.child("concepts");
        conceptCloudEndPoint.addValueEventListener(genericListener);

        recViewChallenges = findViewById(R.id.recViewChallenge);
        recViewProblems = findViewById(R.id.recViewProblem);
        recViewIdeas = findViewById(R.id.recViewIdea);
        recViewConcepts = findViewById(R.id.recyclerViewConcept);

        challenges = new ArrayList<>();
        challengesTitles = new ArrayList<>();
        problems = new ArrayList<>();
        problemsTitles = new ArrayList<>();
        ideas = new ArrayList<>();
        ideasTitles = new ArrayList<>();
        concepts = new ArrayList<>();
        conceptsTitles = new ArrayList<>();

        recViewChallenges.setHasFixedSize(true);
        recViewProblems.setHasFixedSize(true);
        recViewIdeas.setHasFixedSize(true);
        recViewConcepts.setHasFixedSize(true);

        recViewLayoutManagerChallenges = new LinearLayoutManager(this);
        recViewLayoutManagerProblems = new LinearLayoutManager(this);
        recViewLayoutManagerIdeas = new LinearLayoutManager(this);
        recViewLayoutManagerConcepts = new LinearLayoutManager(this);

        recViewAdapterChallenges = new MainAdapter(challengesTitles);
        recViewChallenges.setLayoutManager(recViewLayoutManagerChallenges);
        recViewChallenges.setAdapter(recViewAdapterChallenges);

        recViewAdapterProblems = new MainAdapter(problemsTitles);
        recViewProblems.setLayoutManager(recViewLayoutManagerProblems);
        recViewProblems.setAdapter(recViewAdapterProblems);

        recViewAdapterIdeas = new MainAdapter(ideasTitles);
        recViewIdeas.setLayoutManager(recViewLayoutManagerIdeas);
        recViewIdeas.setAdapter(recViewAdapterIdeas);

        recViewAdapterConcepts = new MainAdapter(conceptsTitles);
        recViewConcepts.setLayoutManager(recViewLayoutManagerConcepts);
        recViewConcepts.setAdapter(recViewAdapterConcepts);

        recViewProblems.addOnItemTouchListener(new RecyclerTouchListener(getApplicationContext(), recViewProblems, new RecyclerTouchListener.ClickListener() {
            @Override
            public void onClick(View view, int position) {
                String title = problemsTitles.get(position);
                Log.d(LOG_TAG, "click from main activity with" + title);
                Problem p = problems.get(position);
                ArrayList <String> linkedIdeasUid = p.getLinkedIdeas();
                ArrayList <String> linkedProblemsUid = new ArrayList<>();
                linkedProblemsUid.add(p.getUid());

                lightupIdeas(linkedIdeasUid);
                lightupProblems(linkedProblemsUid);
            }

            @Override
            public void onLongClick(View view, int position) {
                Log.d(LOG_TAG, "long click from main activity");

            }
        }));

        recViewIdeas.addOnItemTouchListener(new RecyclerTouchListener(getApplicationContext(), recViewIdeas, new RecyclerTouchListener.ClickListener() {
            @Override
            public void onClick(View view, int position) {
                String title = ideasTitles.get(position);
                Log.d("lightup", "idea clicked from main activity with" + title);
                Idea idea = ideas.get(position);
                Log.d("lightup", "idea clicked " + idea.toString());
                ArrayList <String> linkedProblemsUid = idea.getLinkedProblems();
                ArrayList <String> linkedConceptsUid = idea.getLinkedConcepts();
                ArrayList <String> ideaUid = new ArrayList<>();
                ideaUid.add(idea.getUid());

                lightupIdeas(ideaUid);
                lightupProblems(linkedProblemsUid);
                lightupConcepts(linkedConceptsUid);
            }

            @Override
            public void onLongClick(View view, int position) {
                Log.d(LOG_TAG, "long click from main activity");

            }
        }));

    }

    private void lightupProblems(ArrayList<String> problemUids) {
        //clear first
        int max = recViewLayoutManagerProblems.getItemCount();
        for(int i = 0; i < max; i++) {
            unHighlightView(recViewProblems, i);
        }
        if(problemUids == null) {
            Log.d("lightup", "no problems to light up :-(");
            return;
        }
        Log.d("lightup", "problems to light up" + problemUids.toString());
        for(String uid : problemUids) {
            // fetch all problems with this uid
            Problem p = getProblem(uid);
            int ii = problems.indexOf(p);
            highlightView(recViewProblems, ii);
        }
    }

    private void lightupIdeas(ArrayList<String> ideaUids) {
        //clear first
        int max = recViewLayoutManagerIdeas.getItemCount();
        for(int i = 0; i < max; i++) {
            unHighlightView(recViewIdeas, i);
        }
        if(ideaUids == null) {
            return;
        }

        for(String uid : ideaUids) {
            // fetch idea with this uid
            Idea idea = getIdea(uid);
            int ii = ideas.indexOf(idea);
            highlightView(recViewIdeas, ii);
        }
    }

    private void lightupConcepts(ArrayList<String> conceptUids) {
        //clear first
        int max = recViewLayoutManagerConcepts.getItemCount();
        for(int i = 0; i < max; i++) {
            unHighlightView(recViewConcepts, i);
        }
        if(conceptUids == null) {
            Log.d("lightup", "no concepts to light up :-(");
            return;
        }
        Log.d("lightup", "concepts to light up" + conceptUids.toString());
        for(String uid : conceptUids) {
            // fetch all problems with this uid
            Concept c = getConcept(uid);
            int ii = concepts.indexOf(c);
            highlightView(recViewConcepts, ii);
        }
    }

    private void highlightView(RecyclerView rc, int ii) {
        rc.findViewHolderForAdapterPosition(ii).itemView.
                setBackgroundColor(Color.parseColor("#668cff"));
        TextView tv = rc.findViewHolderForAdapterPosition(ii).itemView.findViewById(R.id.tvTitle);
        tv.setTypeface(null, Typeface.BOLD);
        tv.setBackgroundColor(Color.parseColor("#ccd9ff"));
    }
    private void unHighlightView(RecyclerView rc, int i) {
        TextView tv = rc.findViewHolderForAdapterPosition(i).itemView.findViewById(R.id.tvTitle);
        rc.findViewHolderForAdapterPosition(i).itemView.
                setBackgroundColor(Color.parseColor("#ffffff"));
        tv.setTypeface(null, Typeface.NORMAL);
        tv.setBackgroundColor(Color.parseColor("#ffffff"));
    }


    private Problem getProblem(String uid) {
        for(Problem p : problems) {
            if(p.getUid().equals(uid)) {
                return p;
            }
        }
        return null;
    }

    private Idea getIdea(String uid) {
        for(Idea i : ideas) {
            if(i.getUid().equals(uid)) {
                return i;
            }
        }
        return null;
    }

    private Concept getConcept(String uid) {
        for(Concept c : concepts) {
            if(c.getUid().equals(uid)) {
                return c;
            }
        }
        return null;
    }

    // DATA HANDLING BELOW

    ValueEventListener genericListener = new ValueEventListener() {
        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {
            // Get Post object and use the values to update the UI
            Iterable<DataSnapshot> stuff = dataSnapshot.getChildren();
            String type = dataSnapshot.getKey();
            if(type.equals("challenges")) {
                populateChallenges(stuff);
                recViewAdapterChallenges.notifyDataSetChanged();
            }
            if(type.equals("problems")) {
                populateProblems(stuff);
                recViewAdapterProblems.notifyDataSetChanged();
            }
            if(type.equals("ideas")) {
                populateIdeas(stuff);
                recViewAdapterIdeas.notifyDataSetChanged();
            }
            if(type.equals("concepts")) {
                populateConcepts(stuff);
                recViewAdapterConcepts.notifyDataSetChanged();
            }
        }

        @Override
        public void onCancelled(DatabaseError databaseError) {
            // Getting Post failed, log a message
            Log.w(LOG_TAG, "loadPost:onCancelled", databaseError.toException());
        }
    };

    private void populateChallenges(Iterable<DataSnapshot> stuff) {
        challenges.clear();
        challengesTitles.clear();
        for (DataSnapshot s : stuff) {
            Challenge c = s.getValue(Challenge.class);
            Log.d(LOG_TAG, "read c as " + c.toString());
            challenges.add(c);
            challengesTitles.add(c.getTitle());
        }
    }

    private void populateProblems(Iterable<DataSnapshot> stuff) {
        problems.clear();
        problemsTitles.clear();
        for (DataSnapshot s : stuff) {
            Problem c = s.getValue(Problem.class);
            Log.d(LOG_TAG, "read c as " + c.toString());
            problems.add(c);
            problemsTitles.add(c.getTitle());
        }
    }

    private void populateIdeas(Iterable<DataSnapshot> stuff) {
        ideas.clear();
        ideasTitles.clear();
        for (DataSnapshot s : stuff) {
            Idea c = s.getValue(Idea.class);
            Log.d(LOG_TAG, "read c as " + c.toString());
            ideas.add(c);
            ideasTitles.add(c.getTitle());
        }
    }

    private void populateConcepts(Iterable<DataSnapshot> stuff) {
        concepts.clear();
        conceptsTitles.clear();
        for (DataSnapshot s : stuff) {
            Concept c = s.getValue(Concept.class);
            Log.d(LOG_TAG, "read c as " + c.toString());
            concepts.add(c);
            conceptsTitles.add(c.getTitle());
        }
    }



}