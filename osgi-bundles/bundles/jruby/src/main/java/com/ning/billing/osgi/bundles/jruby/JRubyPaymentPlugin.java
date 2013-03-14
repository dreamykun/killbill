/*
 * Copyright 2010-2013 Ning, Inc.
 *
 * Ning licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.ning.billing.osgi.bundles.jruby;

import java.math.BigDecimal;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.UUID;

import org.jruby.Ruby;
import org.jruby.embed.ScriptingContainer;
import org.jruby.javasupport.JavaEmbedUtils;
import org.jruby.runtime.builtin.IRubyObject;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.log.LogService;

import com.ning.billing.osgi.api.config.PluginRubyConfig;
import com.ning.billing.payment.api.PaymentMethodPlugin;
import com.ning.billing.payment.plugin.api.PaymentInfoPlugin;
import com.ning.billing.payment.plugin.api.PaymentMethodInfoPlugin;
import com.ning.billing.payment.plugin.api.PaymentPluginApi;
import com.ning.billing.payment.plugin.api.PaymentPluginApiException;
import com.ning.billing.payment.plugin.api.RefundInfoPlugin;
import com.ning.billing.util.callcontext.CallContext;
import com.ning.billing.util.callcontext.TenantContext;

public class JRubyPaymentPlugin extends JRubyPlugin implements PaymentPluginApi {

    private volatile ServiceRegistration<PaymentPluginApi> paymentInfoPluginRegistration;

    public JRubyPaymentPlugin(final PluginRubyConfig config, final ScriptingContainer container,
                              final BundleContext bundleContext, final LogService logger) {
        super(config, container, bundleContext, logger);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void startPlugin(final BundleContext context) {
        super.startPlugin(context);

        final Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put("name", pluginMainClass);
        paymentInfoPluginRegistration = (ServiceRegistration<PaymentPluginApi>) context.registerService(PaymentPluginApi.class.getName(), this, props);
    }

    @Override
    public void stopPlugin(final BundleContext context) {
        paymentInfoPluginRegistration.unregister();
        super.stopPlugin(context);
    }

    @Override
    public String getName() {
        return pluginMainClass;
    }


    @Override
    public PaymentInfoPlugin processPayment(final UUID kbPaymentId, final UUID kbPaymentMethodId, final BigDecimal amount, final CallContext context) throws PaymentPluginApiException {

        return callWithRuntimeAndChecking(new PluginCallback() {
            @Override
            public PaymentInfoPlugin doCall(final Ruby runtime) {
                final IRubyObject res = pluginInstance.callMethod("charge",
                                                                  JavaEmbedUtils.javaToRuby(runtime, kbPaymentId.toString()),
                                                                  JavaEmbedUtils.javaToRuby(runtime, kbPaymentMethodId.toString()),
                                                                  JavaEmbedUtils.javaToRuby(runtime, amount.longValue() * 100));
                return null;
            }
        });
    }

    @Override
    public PaymentInfoPlugin getPaymentInfo(final UUID kbPaymentId, final TenantContext context) throws PaymentPluginApiException {

        return callWithRuntimeAndChecking(new PluginCallback() {
            @Override
            public PaymentInfoPlugin doCall(final Ruby runtime) {
                final IRubyObject res = pluginInstance.callMethod("get_payment_info", JavaEmbedUtils.javaToRuby(getRuntime(), kbPaymentId.toString()));
                return null;
            }
        });
    }

    @Override
    public RefundInfoPlugin processRefund(final UUID kbPaymentId, final BigDecimal refundAmount, final CallContext context) throws PaymentPluginApiException {

        return callWithRuntimeAndChecking(new PluginCallback() {
            @Override
            public RefundInfoPlugin doCall(final Ruby runtime) {
                final IRubyObject res = pluginInstance.callMethod("refund",
                                                                  JavaEmbedUtils.javaToRuby(runtime, kbPaymentId.toString()),
                                                                  JavaEmbedUtils.javaToRuby(runtime, refundAmount.longValue() * 100));

                return null;
            }
        });

    }

    @Override
    public void addPaymentMethod(final UUID kbAccountId, final UUID kbPaymentMethodId, final PaymentMethodPlugin paymentMethodProps, final boolean setDefault, final CallContext context) throws PaymentPluginApiException {

        callWithRuntimeAndChecking(new PluginCallback() {
            @Override
            public Void doCall(final Ruby runtime) {
                final IRubyObject res = pluginInstance.callMethod("add_payment_method",
                                                                  JavaEmbedUtils.javaToRuby(runtime, kbAccountId.toString()),
                                                                  JavaEmbedUtils.javaToRuby(runtime, kbPaymentMethodId.toString()),
                                                                  JavaEmbedUtils.javaToRuby(runtime, paymentMethodProps));

                return null;
            }
        });
    }

    @Override
    public void deletePaymentMethod(final UUID kbPaymentMethodId, final CallContext context) throws PaymentPluginApiException {

        callWithRuntimeAndChecking(new PluginCallback() {
            @Override
            public Void doCall(final Ruby runtime) {
                final IRubyObject res = pluginInstance.callMethod("delete_payment_method",
                                                                  JavaEmbedUtils.javaToRuby(runtime, kbPaymentMethodId.toString()));

                return null;
            }
        });
    }

    @Override
    public PaymentMethodPlugin getPaymentMethodDetail(final UUID kbAccountId, final UUID kbPaymentMethodId, final TenantContext context) throws PaymentPluginApiException {

        return callWithRuntimeAndChecking(new PluginCallback() {
            @Override
            public PaymentMethodPlugin doCall(final Ruby runtime) {
                final IRubyObject res =  pluginInstance.callMethod("get_payment_method_detail",
                                                                   JavaEmbedUtils.javaToRuby(runtime, kbAccountId.toString()),
                                                                   JavaEmbedUtils.javaToRuby(runtime, kbPaymentMethodId.toString()));

                return null;
            }
        });
    }

    @Override
    public void setDefaultPaymentMethod(final UUID kbPaymentMethodId, final CallContext context) throws PaymentPluginApiException {

        callWithRuntimeAndChecking(new PluginCallback() {
            @Override
            public Void doCall(final Ruby runtime) {
                final IRubyObject res = pluginInstance.callMethod("set_default_payment_method",
                                                                  JavaEmbedUtils.javaToRuby(runtime, kbPaymentMethodId.toString()));

                return null;
            }
        });
    }

    @Override
    public List<PaymentMethodInfoPlugin> getPaymentMethods(final UUID kbAccountId, final boolean refreshFromGateway, final CallContext context) throws PaymentPluginApiException {

        return callWithRuntimeAndChecking(new PluginCallback() {
            @Override
            public List<PaymentMethodInfoPlugin> doCall(final Ruby runtime) {
                final IRubyObject res = pluginInstance.callMethod("get_payment_methods",
                                                                  JavaEmbedUtils.javaToRuby(runtime, kbAccountId.toString()));

                return null;
            }
        });
    }

    @Override
    public void resetPaymentMethods(final List<PaymentMethodInfoPlugin> paymentMethods) throws PaymentPluginApiException {

        callWithRuntimeAndChecking(new PluginCallback() {
            @Override
            public Void doCall(final Ruby runtime) {
                final IRubyObject res = pluginInstance.callMethod("reset_payment_methods",
                                                                  JavaEmbedUtils.javaToRuby(runtime, paymentMethods));
                return null;
            }
        });
    }

    private abstract class PluginCallback {

        public abstract <T> T doCall(final Ruby runtime);

        public boolean checkValidPaymentPlugin() {
            return true;
        }
    }

    private <T> T callWithRuntimeAndChecking(PluginCallback cb) {
        try {
            checkPluginIsRunning();

            if (cb.checkValidPaymentPlugin()) {
                checkValidPaymentPlugin();
            }

            final Ruby runtime = getRuntime();
            return cb.doCall(runtime);

        } catch (RuntimeException e) {
            // TODO STEPH not sure what ruby can throw
            throw e;
        }
    }
}