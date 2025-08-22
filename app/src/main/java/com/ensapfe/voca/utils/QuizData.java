package com.ensapfe.voca.utils;

import com.ensapfe.voca.models.Category;
import com.ensapfe.voca.models.Question;
import java.util.ArrayList;
import java.util.List;

public class QuizData {

    public static List<Category> getAllCategoriesWithQuestions() {
        List<Category> categories = new ArrayList<>();

        Category cat1 = new Category();
        cat1.setId("1");
        cat1.setName("Grammar");
        cat1.setQuestions(getGrammarQuestions());

        Category cat2 = new Category();
        cat2.setId("2");
        cat2.setName("Vocabulary");
        cat2.setQuestions(getVocabularyQuestions());

        Category cat3 = new Category();
        cat3.setId("3");
        cat3.setName("Reading Comprehension");
        cat3.setQuestions(getReadingComprehensionQuestions());

        Category cat4 = new Category();
        cat4.setId("4");
        cat4.setName("Synonyms & Antonyms");
        cat4.setQuestions(getSynonymsAntonymsQuestions());

        Category cat5 = new Category();
        cat5.setId("5");
        cat5.setName("Common Mistakes");
        cat5.setQuestions(getCommonMistakesQuestions());

        Category cat6 = new Category();
        cat6.setId("6");
        cat6.setName("Tenses");
        cat6.setQuestions(getTensesQuestions());

        categories.add(cat1);
        categories.add(cat2);
        categories.add(cat3);
        categories.add(cat4);
        categories.add(cat5);
        categories.add(cat6);


        return categories;
    }

    private static List<Question> getGrammarQuestions() {
        List<Question> questions = new ArrayList<>();

        Question q1 = new Question();
        q1.setQuestion("Which sentence is correct?");
        q1.setOptions(List.of("He go to school every day", "He goes to school every day", "He going to school every day", "He gone to school every day"));
        q1.setAnswerIndex(3);

        Question q2 = new Question();
        q2.setQuestion("Choose the correct verb form: “They ___ playing football.”");
        q2.setOptions(List.of("is", "am", "are", "be"));
        q2.setAnswerIndex(3);

        Question q3 = new Question();
        q3.setQuestion("Which word is a conjunction?");
        q3.setOptions(List.of("Quickly", "And", "Teacher", "Beautiful"));
        q3.setAnswerIndex(2);

        Question q4 = new Question();
        q4.setQuestion("Choose the correct sentence.");
        q4.setOptions(List.of("She don’t like apples", "She doesn’t likes apples", "She doesn’t like apples", "She not like apples"));
        q4.setAnswerIndex(3);

        Question q5 = new Question();
        q5.setQuestion("Which pronoun is correct: “___ is my book.”");
        q5.setOptions(List.of("Me", "I", "This", "Mine"));
        q5.setAnswerIndex(3);

        questions.add(q1);
        questions.add(q2);
        questions.add(q3);
        questions.add(q4);
        questions.add(q5);

        return questions;
    }

    private static List<Question> getVocabularyQuestions() {
        List<Question> questions = new ArrayList<>();

        Question q1 = new Question();
        q1.setQuestion("What is the opposite of “Hot”?");
        q1.setOptions(List.of("Warm","Cold","Cool","Heat"));
        q1.setAnswerIndex(2);

        Question q2 = new Question();
        q2.setQuestion("Which word means “happy”?");
        q2.setOptions(List.of("Sad","Angry","Joyful","Tired"));
        q2.setAnswerIndex(3);

        Question q3 = new Question();
        q3.setQuestion("Which is a fruit?");
        q3.setOptions(List.of("Tomato","Banana","Carrot","Potato"));
        q3.setAnswerIndex(2);

        Question q4 = new Question();
        q4.setQuestion("Which word means “big”?");
        q4.setOptions(List.of("Small","Large","Tiny","Thin"));
        q4.setAnswerIndex(2);

        Question q5 = new Question();
        q5.setQuestion("Which is a synonym for “fast”?");
        q5.setOptions(List.of("Slow","Rapid","Late","Still"));
        q5.setAnswerIndex(2);

        questions.add(q1);
        questions.add(q2);
        questions.add(q3);
        questions.add(q4);
        questions.add(q5);

        return questions;
    }

    private static List<Question> getSynonymsAntonymsQuestions() {
        List<Question> questions = new ArrayList<>();

        Question q1 = new Question();
        q1.setQuestion("questions.add(q2);");
        q1.setOptions(List.of("Start", "Stop", "End", "Finish"));
        q1.setAnswerIndex(1);

        Question q2 = new Question();
        q2.setQuestion("Which is an antonym of “Cold”?");
        q2.setOptions(List.of("Hot", "Freeze", "Ice", "Warmth"));
        q2.setAnswerIndex(1);

        Question q3 = new Question();
        q3.setQuestion("Which is a synonym of “Small”?");
        q3.setOptions(List.of("Large", "Tiny", "Huge", "Tall"));
        q3.setAnswerIndex(2);

        Question q4 = new Question();
        q4.setQuestion("Which is an antonym of “Happy”?");
        q4.setOptions(List.of("Sad", "Joyful", "Excited", "Smiling"));
        q4.setAnswerIndex(1);

        Question q5 = new Question();
        q5.setQuestion("Which is a synonym of “Fast”?");
        q5.setOptions(List.of("Rapid", "Slow", "Sluggish", "Late"));
        q5.setAnswerIndex(1);

        questions.add(q1);
        questions.add(q2);
        questions.add(q3);
        questions.add(q4);
        questions.add(q5);

        return questions;
    }
    private static List<Question> getCommonMistakesQuestions() {
        List<Question> questions = new ArrayList<>();

        Question q1 = new Question();
        q1.setQuestion("Which sentence is correct?");
        q1.setOptions(List.of("She don’t like tea", "She doesn’t like tea", "She no like tea", "She not likes tea"));
        q1.setAnswerIndex(2);

        Question q2 = new Question();
        q2.setQuestion("Choose the correct form: “I have ___ my homework.”");
        q2.setOptions(List.of("do", "did", "done", "does"));
        q2.setAnswerIndex(3);

        Question q3 = new Question();
        q3.setQuestion("Which sentence is correct?");
        q3.setOptions(List.of("He can to swim", "He can swim", "He cans swim", "He swim can"));
        q3.setAnswerIndex(2);

        Question q4= new Question();
        q4.setQuestion("Choose the correct spelling.");
        q4.setOptions(List.of("Enviroment", "Environment", "Environmant", "Envaironment"));
        q4.setAnswerIndex(2);

        Question q5 = new Question();
        q5.setQuestion("Which is correct?");
        q5.setOptions(List.of("I am agree", "I agree", "I agrees", "I am agreed"));
        q5.setAnswerIndex(3);

        questions.add(q1);
        questions.add(q2);
        questions.add(q3);
        questions.add(q4);
        questions.add(q5);

        return questions;
    }
    private static List<Question> getReadingComprehensionQuestions() {
        List<Question> questions = new ArrayList<>();

        Question q1 = new Question();
        q1.setQuestion("What does Anna do first?");
        q1.setOptions(List.of("Eats breakfast", "Wakes up", "Goes to school", "Plays with friends"));
        q1.setAnswerIndex(2);

        Question q2 = new Question();
        q2.setQuestion("Where does Anna go after breakfast?");
        q2.setOptions(List.of("School", "Park", "Work", "Library"));
        q2.setAnswerIndex(3);

        Question q3 = new Question();
        q3.setQuestion("When does Anna play with friends?");
        q3.setOptions(List.of("Morning", "Lunch", "Evening", "Night"));
        q3.setAnswerIndex(2);

        Question q4 = new Question();
        q4.setQuestion("What does Anna like to do at lunch?");
        q4.setOptions(List.of("Study", "Play", "Sleep", "Eat"));
        q4.setAnswerIndex(2);

        Question q5 = new Question();
        q5.setQuestion("Which is true?");
        q5.setOptions(List.of("Anna wakes up late", "Anna goes to school", "Anna works in the park", "Anna plays in the morning"));
        q5.setAnswerIndex(2);

        questions.add(q1);
        questions.add(q2);
        questions.add(q3);
        questions.add(q4);
        questions.add(q5);

        return questions;
    }
    private static List<Question> getTensesQuestions() {
        List<Question> questions = new ArrayList<>();

        Question q1 = new Question();
        q1.setQuestion("Which sentence is in the past tense?");
        q1.setOptions(List.of("She plays football", "She will play football", "She played football", "She is playing football"));
        q1.setAnswerIndex(3);

        Question q2 = new Question();
        q2.setQuestion("Choose the future tense:");
        q2.setOptions(List.of("They will visit the museum", "They visit the museum", "They are visiting the museum", "They visited the museum"));
        q2.setAnswerIndex(1);

        Question q3 = new Question();
        q3.setQuestion("Which is present continuous tense?");
        q3.setOptions(List.of("He is reading a book", "He read a book", "He will read a book", "He reads a book"));
        q3.setAnswerIndex(2);

        Question q4 = new Question();
        q4.setQuestion("Which is past continuous tense?");
        q4.setOptions(List.of("I am eating dinner", "I was eating dinne", "I eat dinner", "I will eat dinner"));
        q4.setAnswerIndex(2);

        Question q5 = new Question();
        q5.setQuestion("Choose the correct sentence:");
        q5.setOptions(List.of("She will goes to the park", "She go to the park", "She will go to the park", "She gone to the park"));
        q5.setAnswerIndex(3);

        questions.add(q1);
        questions.add(q2);
        questions.add(q3);
        questions.add(q4);
        questions.add(q5);

        return questions;
    }
}