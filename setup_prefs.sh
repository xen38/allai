#!/system/bin/sh
run-as com.allai.app.debug sh -c 'echo "{\"onboarding_complete\":true}" > /data/data/com.allai.app.debug/shared_prefs/allai_onboarding.xml'
run-as com.allai.app.debug sh -c 'echo "{\"api_key\":\"test-key\",\"provider_name\":\"openai\",\"model\":\"gpt-4o-mini\",\"base_url\":\"https://api.openai.com/v1/chat/completions\",\"system_prompt\":\"You are Allai\"}" > /data/data/com.allai.app.debug/shared_prefs/allai_settings.xml'
cat /data/data/com.allai.app.debug/shared_prefs/allai_onboarding.xml
cat /data/data/com.allai.app.debug/shared_prefs/allai_settings.xml
