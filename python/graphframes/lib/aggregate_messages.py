#
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

from typing import Any, Type, Tuple
from pyspark.sql import DataFrame, functions as sqlfunctions, SparkSession
from pyspark.sql.types import Column

def _get_java_api() -> Tuple[Any, SparkSession]:
    """Get Java API and active SparkSession."""
    spark = SparkSession.getActiveSession()
    if spark is None:
        raise RuntimeError("No active SparkSession found")
    javaClassName = "org.graphframes.GraphFramePythonAPI"
    jvm_gf_api = spark._sc._jvm.Thread.currentThread().getContextClassLoader().loadClass(javaClassName) \
            .newInstance()
    return jvm_gf_api, spark

class AggregateMessages:
    """Collection of utilities usable with :meth:`graphframes.GraphFrame.aggregateMessages()`."""

    @classmethod
    def src(cls) -> Column:
        """Reference for source column, used for specifying messages."""
        jvm_gf_api, _ = _get_java_api()
        return sqlfunctions.col(jvm_gf_api.SRC())

    @classmethod
    def dst(cls) -> Column:
        """Reference for destination column, used for specifying messages."""
        jvm_gf_api, _ = _get_java_api()
        return sqlfunctions.col(jvm_gf_api.DST())

    @classmethod
    def edge(cls) -> Column:
        """Reference for edge column, used for specifying messages."""
        jvm_gf_api, _ = _get_java_api()
        return sqlfunctions.col(jvm_gf_api.EDGE())

    @classmethod
    def msg(cls) -> Column:
        """Reference for message column, used for specifying aggregation function."""
        jvm_gf_api, _ = _get_java_api()
        return sqlfunctions.col(jvm_gf_api.aggregateMessages().MSG_COL_NAME())

    @staticmethod
    def getCachedDataFrame(df: DataFrame) -> DataFrame:
        """
        Create a new cached copy of a DataFrame.

        This utility method is useful for iterative DataFrame-based algorithms. See Scala
        documentation for more details.

        WARNING: This is NOT the same as `DataFrame.cache()`.
                 The original DataFrame will NOT be cached.
        """
        jvm_gf_api, spark = _get_java_api()
        jdf = jvm_gf_api.aggregateMessages().getCachedDataFrame(df._jdf)
        return DataFrame(jdf, spark)
