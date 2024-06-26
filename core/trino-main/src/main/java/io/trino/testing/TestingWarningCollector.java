/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.trino.testing;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.errorprone.annotations.ThreadSafe;
import com.google.errorprone.annotations.concurrent.GuardedBy;
import io.trino.execution.warnings.WarningCollector;
import io.trino.execution.warnings.WarningCollectorConfig;
import io.trino.spi.TrinoWarning;
import io.trino.spi.WarningCode;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

@ThreadSafe
public class TestingWarningCollector
        implements WarningCollector
{
    @GuardedBy("this")
    private final Map<WarningCode, TrinoWarning> warnings = new LinkedHashMap<>();
    private final int maxWarnings;

    private final boolean addWarnings;
    private final AtomicInteger warningCode = new AtomicInteger();

    public TestingWarningCollector(WarningCollectorConfig config, TestingWarningCollectorConfig testConfig)
    {
        this.maxWarnings = requireNonNull(config, "config is null").getMaxWarnings();
        addWarnings = testConfig.getAddWarnings();
        // Start warning codes at 1
        for (int warningCode = 1; warningCode <= testConfig.getPreloadedWarnings(); warningCode++) {
            add(createTestWarning(warningCode));
        }
        warningCode.set(testConfig.getPreloadedWarnings());
    }

    @Override
    public synchronized void add(TrinoWarning warning)
    {
        requireNonNull(warning, "warning is null");
        if (warnings.size() < maxWarnings) {
            warnings.putIfAbsent(warning.getWarningCode(), warning);
        }
    }

    @Override
    public synchronized List<TrinoWarning> getWarnings()
    {
        if (addWarnings) {
            add(createTestWarning(warningCode.incrementAndGet()));
        }
        return ImmutableList.copyOf(warnings.values());
    }

    @VisibleForTesting
    public static TrinoWarning createTestWarning(int code)
    {
        // format string below is a hack to construct a vendor specific SQLState value
        // 01 is the class of warning code and 5 is the first allowed vendor defined prefix character
        // See the SQL Standard ISO_IEC_9075-2E_2016 24.1: SQLState for more information
        return new TrinoWarning(new WarningCode(code, format("015%02d", code % 100)), "Test warning " + code);
    }
}
