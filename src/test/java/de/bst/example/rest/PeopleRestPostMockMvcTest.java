package de.bst.example.rest;

import static org.hamcrest.matcher.RegexMatcher.matchesRegex;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.snippet.Attributes.key;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.restdocs.JUnitRestDocumentation;
import org.springframework.restdocs.constraints.ConstraintDescriptions;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import de.bst.example.api.MediaTypesWithVersion;
import de.bst.example.api.People;
import de.bst.example.service.PeopleService;

@RunWith(SpringRunner.class)
@SpringBootTest
@WebAppConfiguration
public class PeopleRestPostMockMvcTest {

	//@// @formatter:off
	private static final String RESPONSE_EXCEPTION_BODY = "{\"message\":\"%s\"}";
	private static final String RESPONSE_ERROR_BODY = "{"
				+ "\"message\":\"%s\","
				+ "\"path\":\"/people\","
				+ "\"logref\":\"%s\""
			+ "}";
	private static final String RESPONSE_ERRORS_BODY = "{"
			+ "\"total\":%s,"
			+ "\"_embedded\":{"
				+ "\"errors\":["
					+ "%s"
				+ "]"
			+ "}"
			+ "}";
	// @formatter:on

	@Rule
	public final JUnitRestDocumentation restDocumentation = new JUnitRestDocumentation("target/generated-snippets");

	@Autowired
	private WebApplicationContext context;

	private MockMvc mockMvc;

	@MockBean
	private PeopleService peopleService;

	private final ConstraintDescriptions description = new ConstraintDescriptions(People.class);

	@Before
	public void setUp() throws Exception {

		this.mockMvc = MockMvcBuilders.webAppContextSetup(this.context).apply(springSecurity())
				.apply(documentationConfiguration(this.restDocumentation)).alwaysDo(document("{method-name}/{step}/")).build();
	}

	@Test
	public void test_post_people_http_201() throws Exception {
		// Given
		final String id = UUID.randomUUID().toString();
		final String newPeople = "{\"name\":\"Neuer\",\"age\":1}";

		// When - Then
		mockMvc.perform(post(PeopleRest.URL_PEOPLE).with(httpBasic("user", "password")).content(String.format(newPeople, id))
				.contentType(MediaTypesWithVersion.PEOPLE_V1_JSON_MEDIATYPE))
				.andExpect(status().isCreated())
				.andExpect(
						header().string("Location", matchesRegex("^/people/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$")))
				.andDo(document("people-post-v1",
						requestFields(
								fieldWithPath("name").description("The name of the people")
										.attributes(key("constraints").value(description.descriptionsForProperty("name"))),
								fieldWithPath("age").description("The age of the people")
										.attributes(key("constraints").value(description.descriptionsForProperty("age"))))));
	}

	@Test
	public void test_post_people_http_400_name_empty() throws Exception {
		// Given
		final String id = UUID.randomUUID().toString();
		final String newPeople = "{\"id\":\"%s\",\"name\":\"\",\"age\":1}";

		// When - Then
		mockMvc.perform(post(PeopleRest.URL_PEOPLE).with(httpBasic("user", "password")).content(String.format(newPeople, id))
				.contentType(MediaTypesWithVersion.PEOPLE_V1_JSON_MEDIATYPE)).andDo(print()).andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaTypesWithVersion.ERROR_JSON_MEDIATYPE))
				.andExpect(content().json(errorJsonName(id)));
	}

	@Test
	public void test_post_people_http_400_name_grt50() throws Exception {
		// Given
		final String id = UUID.randomUUID().toString();
		final String newPeople = "{\"id\":\"%s\",\"age\":\"1\",\"name\":\"aaaaaaaaaabbbbbbbbbbccccccccccddddddddddeeeeeeeeeef\"}";

		// When - Then
		mockMvc.perform(post(PeopleRest.URL_PEOPLE).with(httpBasic("user", "password")).content(String.format(newPeople, id))
				.contentType(MediaTypesWithVersion.PEOPLE_V1_JSON_MEDIATYPE)).andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaTypesWithVersion.ERROR_JSON_MEDIATYPE))
				.andExpect(content().json(errorJsonName(id)));
	}

	@Test
	public void test_post_people_http_400_name_only_abc() throws Exception {
		// Given
		final String id = UUID.randomUUID().toString();
		final String newPeople = "{\"id\":\"%s\",\"age\":\"1\",\"name\":\"323143\"}";

		// When - Then
		mockMvc.perform(post(PeopleRest.URL_PEOPLE).with(httpBasic("user", "password")).content(String.format(newPeople, id))
				.contentType(MediaTypesWithVersion.PEOPLE_V1_JSON_MEDIATYPE)).andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaTypesWithVersion.ERROR_JSON_MEDIATYPE))
				.andExpect(content().json(errorJsonName(id)));
	}

	@Test
	public void test_post_people_http_400_age_less_1() throws Exception {
		// Given
		final String id = UUID.randomUUID().toString();
		final String newPeople = "{\"id\":\"%s\",\"age\":0,\"name\":\"Hans\"}";

		// When - Then
		mockMvc.perform(post(PeopleRest.URL_PEOPLE).with(httpBasic("user", "password")).content(String.format(newPeople, id))
				.contentType(MediaTypesWithVersion.PEOPLE_V1_JSON_MEDIATYPE)).andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaTypesWithVersion.ERROR_JSON_MEDIATYPE))
				.andExpect(content().json(errorJsonAge(id)));
	}

	@Test
	public void test_post_people_http_400_age_grt_199() throws Exception {
		// Given
		final String id = UUID.randomUUID().toString();
		final String newPeople = "{\"id\":\"%s\",\"age\":200,\"name\":\"Hans\"}";

		// When - Then
		mockMvc.perform(post(PeopleRest.URL_PEOPLE).with(httpBasic("user", "password")).content(String.format(newPeople, id))
				.contentType(MediaTypesWithVersion.PEOPLE_V1_JSON_MEDIATYPE)).andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaTypesWithVersion.ERROR_JSON_MEDIATYPE))
				.andExpect(content().json(errorJsonAge(id)));
	}

	@Test
	public void test_post_people_http_400_more_validation_issues() throws Exception {
		// Given
		final String id = UUID.randomUUID().toString();
		final String newPeople = "{\"id\":\"%s\",\"age\":200,\"name\":\"\"}";

		// When - Then
		mockMvc.perform(post(PeopleRest.URL_PEOPLE).with(httpBasic("user", "password")).content(String.format(newPeople, id))
				.contentType(MediaTypesWithVersion.PEOPLE_V1_JSON_MEDIATYPE)).andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaTypesWithVersion.ERROR_JSON_MEDIATYPE))
				.andExpect(content().json(String.format(RESPONSE_ERRORS_BODY, 2,
						new StringBuilder().append(errorJsonName(id)).append(",").append(errorJsonAge(id)).toString())));
	}

	@Test
	public void test_post_people_http_401() throws Exception {
		// When - Then
		mockMvc.perform(post(PeopleRest.URL_PEOPLE, new Object())).andExpect(status().isUnauthorized());
	}

	@Test
	public void test_post_people_http_415() throws Exception {
		// Given
		final String id = UUID.randomUUID().toString();
		final String newPeople = "{\"id\":\"%s\",\"name\":\"212dsf3\"}";

		// When - Then
		mockMvc.perform(post(PeopleRest.URL_PEOPLE).with(httpBasic("user", "password")).content(String.format(newPeople, id))
				.contentType(MediaType.APPLICATION_JSON)).andExpect(status().isUnsupportedMediaType());
	}

	@Test
	public void test_post_people_http_500_on_runtimeexception() throws Exception {
		// Given
		final String id = UUID.randomUUID().toString();
		final String newPeople = "{\"id\":\"%s\",\"age\":100,\"name\":\"Bastian\"}";
		doThrow(new RuntimeException("Hard error!")).when(peopleService).add(any());

		// When - Then
		mockMvc.perform(post(PeopleRest.URL_PEOPLE).with(httpBasic("user", "password")).content(String.format(newPeople, id))
				.contentType(MediaTypesWithVersion.PEOPLE_V1_JSON_MEDIATYPE)).andExpect(status().isInternalServerError())
				.andExpect(content().contentTypeCompatibleWith(MediaTypesWithVersion.ERROR_JSON_MEDIATYPE))
				.andExpect(content().json(String.format(RESPONSE_EXCEPTION_BODY, "RuntimeException: Hard error!")));
	}

	private String errorJsonName(String id) {
		return String.format(RESPONSE_ERROR_BODY, "name must match '[a-zA-Z]{1,50}'", id);
	}

	private String errorJsonAge(String id) {
		return String.format(RESPONSE_ERROR_BODY, "age must be between 1 and 199", id);
	}
}
