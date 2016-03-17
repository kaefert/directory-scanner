package com.googlecode.directory_scanner.domain;

public enum FindFilter {
	UNFILTERED {
		@Override
		public String getSQL() {
			return ")";
		}
	},
	OLDER {
		@Override
		public String getSQL() {
			return " AND f2.lastmodified < f1.lastmodified )";
		}
	},
	OLDEREQUALS {
		@Override
		public String getSQL() {
			return " AND f2.lastmodified <= f1.lastmodified )";
		}
	},
	EQUALS {
		@Override
		public String getSQL() {
			return " AND f2.lastmodified = f1.lastmodified )";
		}
	},

	NEWEREQUALS {
		@Override
		public String getSQL() {
			return " AND f2.lastmodified >= f1.lastmodified )";
		}
	},

	NEWER {
		@Override
		public String getSQL() {
			return " AND f2.lastmodified > f1.lastmodified )";
		}
	};

	public abstract String getSQL();
}