package com.wimdeblauwe.examples.todomvcthymeleaf.todoitem.web;

import com.wimdeblauwe.examples.todomvcthymeleaf.todoitem.TodoItem;
import com.wimdeblauwe.examples.todomvcthymeleaf.todoitem.TodoItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.xpath;

@SpringBootTest
@AutoConfigureMockMvc
class TodoItemControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TodoItemRepository repository;

    @BeforeEach
    void clearItems() {
        repository.deleteAll();
    }

    @Test
    void emptyListHidesMainAndFooter() throws Exception {
        mockMvc.perform(get("/"))
               .andExpect(status().isOk())
               .andExpect(xpath("//form[@action='/' and @method='post']//input[@name='title']").exists())
               .andExpect(xpath("//section[@class='main']").doesNotExist())
               .andExpect(xpath("//footer[@class='footer']").doesNotExist());
    }

    @Test
    void postingAnItemPersistsItAndRedirectsToTheList() throws Exception {
        mockMvc.perform(post("/").param("title", "Вивчити Thymeleaf"))
               .andExpect(status().is3xxRedirection())
               .andExpect(redirectedUrl("/"));

        assertThat(repository.findAll()).singleElement().satisfies(item -> {
            assertThat(item.getId()).isNotNull();
            assertThat(item.getTitle()).isEqualTo("Вивчити Thymeleaf");
            assertThat(item.isCompleted()).isFalse();
        });

        mockMvc.perform(get("/"))
               .andExpect(status().isOk())
               .andExpect(xpath("//ul[@class='todo-list']/li").nodeCount(1))
               .andExpect(xpath("//ul[@class='todo-list']/li/div/label").string("Вивчити Thymeleaf"))
               .andExpect(xpath("//footer[@class='footer']").exists())
               .andExpect(xpath("//input[@class='new-todo']/@value").string(""));
    }

    @Test
    void rendersStoredTitlesAndRemovesTemplateExamples() throws Exception {
        repository.save(new TodoItem("Read the guide", false));
        repository.save(new TodoItem("<script>alert('todo')</script>", false));

        mockMvc.perform(get("/"))
               .andExpect(status().isOk())
               .andExpect(xpath("//ul[@class='todo-list']/li").nodeCount(2))
               .andExpect(content().string(containsString("Read the guide")))
               .andExpect(content().string(containsString("&lt;script&gt;")))
               .andExpect(content().string(not(containsString("Taste JavaScript"))))
               .andExpect(content().string(not(containsString("Buy a unicorn"))))
               .andExpect(xpath("//ul[@class='todo-list']//input[@class='toggle' and @checked]").doesNotExist());
    }

    @Test
    void blankTitlesAreRejectedWithoutSaving() throws Exception {
        for (String title : new String[]{"", "   "}) {
            mockMvc.perform(post("/").param("title", title))
                   .andExpect(status().isBadRequest());
        }
        mockMvc.perform(post("/"))
               .andExpect(status().isBadRequest());

        assertThat(repository.count()).isZero();
    }

    @Test
    void webJarAssetsAreServedAtTheVersionlessTemplatePaths() throws Exception {
        for (String asset : new String[]{
                "/webjars/todomvc-common/base.css",
                "/webjars/todomvc-app-css/index.css",
                "/webjars/todomvc-common/base.js"}) {
            mockMvc.perform(get(asset))
                   .andExpect(status().isOk())
                   .andExpect(result -> assertThat(result.getResponse().getContentAsByteArray()).isNotEmpty());
        }
    }
}
