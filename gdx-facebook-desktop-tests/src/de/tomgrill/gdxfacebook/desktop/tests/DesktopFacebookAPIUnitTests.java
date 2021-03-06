/*******************************************************************************
 * Copyright 2015 See AUTHORS file.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package de.tomgrill.gdxfacebook.desktop.tests;

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import javafx.application.Application;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Net;
import com.badlogic.gdx.Net.HttpRequest;
import com.badlogic.gdx.Net.HttpResponse;
import com.badlogic.gdx.Net.HttpResponseListener;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration;
import com.badlogic.gdx.net.HttpStatus;

import de.tomgrill.gdxfacebook.core.FacebookConfig;
import de.tomgrill.gdxfacebook.core.ResponseError;
import de.tomgrill.gdxfacebook.core.ResponseListener;
import de.tomgrill.gdxfacebook.desktop.DesktopFacebookAPI;
import de.tomgrill.gdxfacebook.desktop.JXBrowserDesktopFacebookGUI;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ Application.class })
public class DesktopFacebookAPIUnitTests {

	private DesktopFacebookAPI fixture;

	private FacebookConfig config;

	private JXBrowserDesktopFacebookGUI browserMock;

	@Before
	public void setup() {

		/* Required to prevent real browser from starting when testig */
		PowerMockito.mockStatic(Application.class);
		mock(Application.class);

		browserMock = mock(JXBrowserDesktopFacebookGUI.class);

		config = new FacebookConfig();

		HeadlessApplicationConfiguration conf = new HeadlessApplicationConfiguration();
		new HeadlessApplication(new ApplicationAdapter() {
		}, conf);

		Gdx.app = mock(HeadlessApplication.class);
		Gdx.net = mock(Net.class);

		Preferences prefs = Mockito.mock(Preferences.class);

		when(Gdx.app.getPreferences(anyString())).thenReturn(prefs);
		when(prefs.getString("accessToken", "")).thenReturn("INVALID_TOKEN");

		fixture = new DesktopFacebookAPI(config);
	}

	@Test
	public void verifySigninDoesSignout() {
		DesktopFacebookAPI spy = Mockito.spy(fixture);
		ResponseListenerStub listener = new ResponseListenerStub();
		spy.signin(listener);
		verify(spy, times(1)).signout();
	}

	@Test
	public void errorCode_EC_EMPTY_ACCESS_TOKEN_whenNoAccessTokenAndSilentLogin() {
		ResponseListener listener = mock(ResponseListener.class);
		fixture.signin(false, listener);
		verify(listener, times(1)).error(argThat(new ArgumentMatcher<ResponseError>() {
			@Override
			public boolean matches(Object argument) {
				ResponseError error = (ResponseError) argument;
				if (error.getCode() == ResponseError.EC_EMPTY_ACCESS_TOKEN) {
					return true;
				}
				return false;
			}
		}));

	}

	@Test
	public void errorCode_EC_CANCELED_noGUIAllowed() {

		doAnswer(new Answer<Object>() {
			public Object answer(InvocationOnMock invocation) {
				Object[] args = invocation.getArguments();
				HttpResponseListener rListener = (HttpResponseListener) args[1];
				rListener.cancelled();
				return null;
			}
		}).when(Gdx.net).sendHttpRequest(any(HttpRequest.class), any(HttpResponseListener.class));

		ResponseListener listener = mock(ResponseListener.class);

		fixture.setAccessToken("fdgsdfgsdgsdf");
		fixture.signin(false, listener);
		verify(listener, times(1)).error(argThat(new ArgumentMatcher<ResponseError>() {
			@Override
			public boolean matches(Object argument) {
				ResponseError error = (ResponseError) argument;
				if (error.getCode() == ResponseError.EC_CANCELED) {
					return true;
				}
				return false;
			}
		}));

	}

	@Test
	public void errorCode_EC_CANCELED_withGUI() {

		doAnswer(new Answer<Object>() {
			public Object answer(InvocationOnMock invocation) {
				Object[] args = invocation.getArguments();
				HttpResponseListener rListener = (HttpResponseListener) args[1];
				rListener.cancelled();
				return null;
			}
		}).when(Gdx.net).sendHttpRequest(any(HttpRequest.class), any(HttpResponseListener.class));

		ResponseListener listener = mock(ResponseListener.class);

		fixture.setAccessToken("fdgsdfgsdgsdf");
		fixture.signin(listener);
		verify(listener, times(1)).error(argThat(new ArgumentMatcher<ResponseError>() {
			@Override
			public boolean matches(Object argument) {
				ResponseError error = (ResponseError) argument;
				if (error.getCode() == ResponseError.EC_CANCELED) {
					return true;
				}
				return false;
			}
		}));

	}

	@Test
	public void errorCode_EC_FAILED_noGUIAllowed() {

		doAnswer(new Answer<Object>() {
			public Object answer(InvocationOnMock invocation) {
				Object[] args = invocation.getArguments();
				HttpResponseListener rListener = (HttpResponseListener) args[1];
				rListener.failed(new Throwable());
				return null;
			}
		}).when(Gdx.net).sendHttpRequest(any(HttpRequest.class), any(HttpResponseListener.class));

		ResponseListener listener = mock(ResponseListener.class);
		fixture.setAccessToken("sdfgsdfgsdg");
		fixture.signin(false, listener);
		verify(listener, times(1)).error(argThat(new ArgumentMatcher<ResponseError>() {
			@Override
			public boolean matches(Object argument) {
				ResponseError error = (ResponseError) argument;
				if (error.getCode() == ResponseError.EC_FAILED) {
					return true;
				}
				return false;
			}
		}));

	}

	@Test
	public void errorCode_EC_FAILED_withGui() {

		doAnswer(new Answer<Object>() {
			public Object answer(InvocationOnMock invocation) {
				Object[] args = invocation.getArguments();
				HttpResponseListener rListener = (HttpResponseListener) args[1];
				rListener.failed(new Throwable());
				return null;
			}
		}).when(Gdx.net).sendHttpRequest(any(HttpRequest.class), any(HttpResponseListener.class));

		ResponseListener listener = mock(ResponseListener.class);
		fixture.setAccessToken("sdfgsdfgsdg");
		fixture.signin(listener);
		verify(listener, times(1)).error(argThat(new ArgumentMatcher<ResponseError>() {
			@Override
			public boolean matches(Object argument) {
				ResponseError error = (ResponseError) argument;
				if (error.getCode() == ResponseError.EC_FAILED) {
					return true;
				}
				return false;
			}
		}));

	}

	@Test
	public void errorCode_EC_BAD_REQUEST_noGUIAllowed() {

		doAnswer(new Answer<Object>() {
			public Object answer(InvocationOnMock invocation) {
				Object[] args = invocation.getArguments();
				HttpResponseListener rListener = (HttpResponseListener) args[1];
				rListener.handleHttpResponse(new HttpResponse() {

					@Override
					public HttpStatus getStatus() {
						return new HttpStatus(401230); // Not 200
					}

					@Override
					public String getResultAsString() {
						return null;
					}

					@Override
					public InputStream getResultAsStream() {
						return null;
					}

					@Override
					public byte[] getResult() {
						return null;
					}

					@Override
					public Map<String, List<String>> getHeaders() {
						return null;
					}

					@Override
					public String getHeader(String name) {
						return null;
					}
				});
				return null;
			}
		}).when(Gdx.net).sendHttpRequest(any(HttpRequest.class), any(HttpResponseListener.class));

		ResponseListener listener = mock(ResponseListener.class);
		fixture.setAccessToken("gsdfgsdfgsdf");

		fixture.signin(false, listener);
		verify(listener, times(1)).error(argThat(new ArgumentMatcher<ResponseError>() {
			@Override
			public boolean matches(Object argument) {
				ResponseError error = (ResponseError) argument;
				if (error.getCode() == ResponseError.EC_BAD_REQUEST) {
					return true;
				}
				return false;
			}
		}));

	}

	@Test
	public void errorCode_EC_BAD_REQUEST_withGUI_processesToGUILoginWithBrowser_AND_succeeds() {

		doAnswer(new Answer<Object>() {
			public Object answer(InvocationOnMock invocation) {
				Object[] args = invocation.getArguments();
				HttpResponseListener rListener = (HttpResponseListener) args[1];
				rListener.handleHttpResponse(new HttpResponse() {

					@Override
					public HttpStatus getStatus() {
						return new HttpStatus(200);
					}

					@Override
					public String getResultAsString() {
						return null;
					}

					@Override
					public InputStream getResultAsStream() {
						return null;
					}

					@Override
					public byte[] getResult() {
						return null;
					}

					@Override
					public Map<String, List<String>> getHeaders() {
						return null;
					}

					@Override
					public String getHeader(String name) {
						return null;
					}
				});
				return null;
			}
		}).when(Gdx.net).sendHttpRequest(any(HttpRequest.class), any(HttpResponseListener.class));

		ResponseListener listener = mock(ResponseListener.class);
		fixture.setAccessToken("gsdfgsdfgsdf");

		fixture.signin(false, listener);
		verify(listener, times(1)).success();

	}

	@Test
	public void errorCode_EC_OK_noGUIAllowed() {

		doAnswer(new Answer<Object>() {
			public Object answer(InvocationOnMock invocation) {
				Object[] args = invocation.getArguments();
				HttpResponseListener rListener = (HttpResponseListener) args[1];
				rListener.handleHttpResponse(new HttpResponse() {

					@Override
					public HttpStatus getStatus() {
						return new HttpStatus(200);
					}

					@Override
					public String getResultAsString() {
						return null;
					}

					@Override
					public InputStream getResultAsStream() {
						return null;
					}

					@Override
					public byte[] getResult() {
						return null;
					}

					@Override
					public Map<String, List<String>> getHeaders() {
						return null;
					}

					@Override
					public String getHeader(String name) {
						return null;
					}
				});
				return null;
			}
		}).when(Gdx.net).sendHttpRequest(any(HttpRequest.class), any(HttpResponseListener.class));

		ResponseListener listener = mock(ResponseListener.class);
		fixture.setAccessToken("hgdfdfsd");
		fixture.signin(false, listener);
		verify(listener, times(1)).success();

	}

	@Test
	public void errorCode_EC_OK_withGUI() {

		doAnswer(new Answer<Object>() {
			public Object answer(InvocationOnMock invocation) {
				Object[] args = invocation.getArguments();
				HttpResponseListener rListener = (HttpResponseListener) args[1];
				rListener.handleHttpResponse(new HttpResponse() {

					@Override
					public HttpStatus getStatus() {
						return new HttpStatus(200);
					}

					@Override
					public String getResultAsString() {
						return null;
					}

					@Override
					public InputStream getResultAsStream() {
						return null;
					}

					@Override
					public byte[] getResult() {
						return null;
					}

					@Override
					public Map<String, List<String>> getHeaders() {
						return null;
					}

					@Override
					public String getHeader(String name) {
						return null;
					}
				});
				return null;
			}
		}).when(Gdx.net).sendHttpRequest(any(HttpRequest.class), any(HttpResponseListener.class));

		ResponseListener listener = mock(ResponseListener.class);
		fixture.setAccessToken("sdfgsdfg");
		fixture.signin(listener);
		verify(listener, times(1)).success();

	}

	@Test
	public void isLoadedIsTrue() {
		assertTrue(fixture.isLoaded());
	}
}
