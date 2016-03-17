package com.googlecode.directory_scanner.domain;

public enum Sort {
	SHA1 {
		@Override
		public String getSQL() {
			return " ORDER BY f.sha1, d.path";
		}
	},
	SIZE {
		@Override
		public String getSQL() {
			return " ORDER BY f.size DESC, f.sha1, d.path";
		}
	},
	COUNT {
		@Override
		public String getSQL() {
			return " ORDER BY (SELECT count(fc.id) FROM files fc WHERE fc.sha1 = f.sha1) DESC, f.sha1, d.path";
		}
	},
	SIZETIMESCOUNT {
		@Override
		public String getSQL() {
			return " ORDER BY f.size*(SELECT count(fc.id) FROM files fc WHERE fc.sha1 = f.sha1) DESC, f.sha1, d.path";
		}
	};

	public abstract String getSQL();
}