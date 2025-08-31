package com.example.hackathonprep;

import java.util.List;

public class GeminiResponse {
    private List<Candidate> candidates;

    public String getTranscription() {
        if (candidates != null && !candidates.isEmpty()) {
            Candidate candidate = candidates.get(0);
            if (candidate != null && candidate.content != null &&
                    candidate.content.parts != null && !candidate.content.parts.isEmpty()) {
                return candidate.content.parts.get(0).text;
            }
        }
        return "";
    }

    public List<Candidate> getCandidates() {
        return candidates;
    }

    public static class Candidate {
        private Content content;

        public Content getContent() {
            return content;
        }

        public static class Content {
            private List<Part> parts;

            public List<Part> getParts() {
                return parts;
            }

            public static class Part {
                private String text;

                public String getText() {
                    return text;
                }
            }
        }
    }
}
